# RAG 检索链路评测工具

对 `RagRetrievalService` 完整检索链路（QueryRewrite → 向量 + 关键词双路 → RRF → Rerank）做量化评测，
产出 `docs/rag-eval-report.md`。仅依赖 Python 3 标准库（urllib），无需 pip 安装；柱状图优先 matplotlib（可选）。

## 前置条件

1. 后端已启动（默认 `http://localhost:8080`）
2. 目标知识库（kbId）已入库文档
3. 一个可用的模型 ID（用于 `build_dataset.py` 生成问题，如 `deepseek-chat`）
4. 有知识库访问权限的账号

## 使用步骤

```bash
# 1. 生成评测数据集（从知识库 chunk 采样 + LLM 生成问题，题目标注来源 chunkId）
python build_dataset.py --kb-id 1 --count 50 --model-id deepseek-chat

# 2. 执行评测（逐题调 retrieve-test 接口，逐阶段计算指标），生成 docs/rag-eval-report.md
python run_eval.py --kb-id 1

# 可选参数
python run_eval.py --kb-id 1 --top-k 5 --report ../../docs/rag-eval-report.md
```

账号与地址可用环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `RAG_EVAL_BASE_URL` | `http://localhost:8080` | 后端地址 |
| `RAG_EVAL_USERNAME` | `rageval` | 登录账号（评测专用账号，见下） |
| `RAG_EVAL_PASSWORD` | `RagEval#2026` | 登录密码 |
| `RAG_EVAL_MODEL_ID` | `deepseek-chat` | 生成问题用的模型 |

> 评测专用账号：若 dev 库中尚无 `rageval` 用户，先调 `POST /api/v1/auth/register`
> 注册（用户名 rageval / 密码 RagEval#2026），再执行脚本。

## 指标口径

一次 retrieve-test 调用返回完整链路 trace，评测脚本**在同一份数据上按阶段切分对比**（对应开源计划中的
5 组对比实验，其中"向量+关键词（无融合排序）"由 RRF 融合排序天然覆盖，不再单独拆分）：

| 阶段 | trace 字段 | 说明 |
|---|---|---|
| 纯向量 | `vectorRecall` | 仅语义向量召回（topKPerQuery 截断） |
| 纯关键词 | `keywordRecall` | 仅全文关键词召回 |
| 向量+关键词+RRF | `rrfMerged` | 双路 RRF 融合排序 |
| +Rerank | `reranked` | RRF 候选集精排后的最终结果 |

- **Recall@5**：top5 命中的标准 chunk 占该题全部标准 chunk 的比例，对全体题目求平均
- **MRR**：第一个标准 chunk 排名倒数的均值（未命中记 0）
- **nDCG@5**：二值相关度下的折损累计增益与理想值之比
- **平均耗时**：链路各步骤耗时之和（服务端记录）+ 客户端整体耗时

## 已知限制

- **Token 消耗**：trace 链路不透出 token 计数（QueryRewrite 的 LLM 调用在服务端计费计入 sys_llm_log），
  报告中该项标注为不可得。
- 标准答案由 LLM 依据 chunk 内容生成，存在极少数题目问题与原文对不上的噪声；`dataset.jsonl` 已入库，
  可人工修订后重跑 `run_eval.py`（不影响已发布数据）。
- 评测走 `retrieve-test` 接口（不走缓存），结果可重复。
