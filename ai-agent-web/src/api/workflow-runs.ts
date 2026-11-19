/**
 * 工作流执行记录 API
 *
 * 对应后端：WfWorkflowController（全局 runs 列表 + run 详情）
 */
import { get } from '@/utils/request'

/** 工作流执行记录（对齐后端 WfWorkflowRun 实体） */
export interface WorkflowRunItem {
  id: number
  runId: string
  workflowId: string
  workflowName?: string
  version: number
  status: 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'PAUSED'
  inputs?: string | null
  outputs?: string | null
  totalTokens?: number
  elapsedMs?: number
  triggeredBy?: string
  createdAt: string
  finishedAt?: string | null
  errorMessage?: string | null
}

/** 节点执行记录（对齐后端 WfNodeRun 实体） */
export interface NodeRunItem {
  id: number
  runId: string
  nodeId: string
  nodeType: string
  title?: string
  status: 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED'
  inputs?: string | null
  outputs?: string | null
  tokens?: number
  elapsedMs?: number
  errorMessage?: string | null
  createdAt?: string
}

/** 全局分页查询执行记录列表 */
export function listAllWorkflowRuns(
  pageNum: number = 1,
  pageSize: number = 20,
  status?: string,
) {
  const params: Record<string, unknown> = { pageNum, pageSize }
  if (status) params.status = status
  return get<{ records: WorkflowRunItem[]; total: number }>('/workflows/runs', params as any)
}

/** 查询执行记录详情（run + 所有 nodeRuns） */
export function getWorkflowRunDetail(runId: string) {
  return get<{ run: WorkflowRunItem; nodes: NodeRunItem[] }>(`/workflows/runs/${runId}`)
}
