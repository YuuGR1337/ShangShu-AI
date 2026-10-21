<script setup lang="ts">
/**
 * 计算机网络课程智能问答页（Web 端）
 *
 * 对接后端 /course/question：意图分类（知识点/习题/闲聊）→ 四路召回（含知识图谱多跳）→ 课程 prompt → 模型回答。
 * JSON 同步返回，页面自管理消息列表。
 */
import { ref, nextTick } from 'vue'
import { ElInput, ElButton } from 'element-plus'
import { askCourseQuestion } from '@/api/course'

interface CourseMessage {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<CourseMessage[]>([])
const inputText = ref('')
const sending = ref(false)
const listRef = ref<HTMLElement | null>(null)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  inputText.value = ''
  messages.value.push({ role: 'user', content: text })
  sending.value = true
  scrollToBottom()

  const history = messages.value
    .slice(0, -1)
    .map((m) => ({ role: m.role, content: m.content }))

  try {
    const answer = await askCourseQuestion({ content: text, history })
    messages.value.push({ role: 'assistant', content: answer || '（无回答）' })
  } catch (err: unknown) {
    messages.value.push({
      role: 'assistant',
      content: `[错误] ${err instanceof Error ? err.message : '回答失败，请重试'}`,
    })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}
</script>

<template>
  <div class="course-view">
    <div class="course-view__header">
      <h2 class="course-view__title">计算机网络课程助手</h2>
      <p class="course-view__subtitle">
        可提问知识点（如"什么是 TCP 三次握手"）或习题（如"这道题怎么做"）
      </p>
    </div>

    <div ref="listRef" class="course-view__list">
      <div v-if="messages.length === 0" class="course-view__empty">
        开始你的第一个课程提问吧
      </div>
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        class="course-msg"
        :class="`course-msg--${msg.role}`"
      >
        <div class="course-msg__content">{{ msg.content }}</div>
      </div>
      <div v-if="sending" class="course-msg course-msg--assistant">
        <div class="course-msg__content">思考中...</div>
      </div>
    </div>

    <div class="course-view__input-bar">
      <el-input
        v-model="inputText"
        placeholder="输入课程问题..."
        :disabled="sending"
        @keyup.enter="handleSend"
      />
      <el-button type="primary" :loading="sending" :disabled="!inputText.trim()" @click="handleSend">
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.course-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.course-view__header {
  margin-bottom: 12px;
}

.course-view__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.course-view__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #9ca3af;
}

.course-view__list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.course-view__empty {
  padding: 80px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.course-msg {
  display: flex;
  margin-bottom: 12px;
}

.course-msg--user {
  justify-content: flex-end;
}

.course-msg--assistant {
  justify-content: flex-start;
}

.course-msg__content {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.course-msg--user .course-msg__content {
  background: #6366f1;
  color: #fff;
}

.course-msg--assistant .course-msg__content {
  background: #f3f4f6;
  color: #1f2937;
}

.course-view__input-bar {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}
</style>
