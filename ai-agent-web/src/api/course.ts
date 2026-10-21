/**
 * 课程知识库问答 API 封装
 *
 * 对接后端 POST /api/v1/course/question（JSON 同步返回）。
 * 后端做意图分类（知识点/习题/闲聊）→ 四路召回 → 课程 prompt → 模型回答。
 */
import { post } from '@/utils/request'

/** 课程问答请求 */
export interface CourseQuestionReq {
  /** 用户问题 */
  content: string
  /** 多轮历史对话（[{role, content}]，可空） */
  history?: Array<{ role: string; content: string }>
  /** 模型 ID（可空，缺省用后端配置） */
  modelId?: string
}

/**
 * 课程智能问答
 * POST /api/v1/course/question
 */
export function askCourseQuestion(data: CourseQuestionReq) {
  return post<string>('/course/question', data)
}
