<template>
  <div class="run-history">
    <el-card class="run-history__card">
      <template #header>
        <div class="run-history__header">
          <span class="run-history__title">工作流执行记录</span>
          <div class="run-history__filter">
            <el-select
              v-model="filterStatus"
              placeholder="全部状态"
              clearable
              size="default"
              style="width: 160px"
              @change="onRefresh"
            >
              <el-option label="运行中" value="RUNNING" />
              <el-option label="成功" value="SUCCEEDED" />
              <el-option label="失败" value="FAILED" />
              <el-option label="暂停" value="PAUSED" />
            </el-select>
            <el-button type="primary" :icon="Refresh" @click="onRefresh">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" v-loading="loading" stripe border>
        <el-table-column prop="runId" label="Run ID" width="220" show-overflow-tooltip />
        <el-table-column prop="workflowId" label="工作流 ID" width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark" round>
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="elapsedMs" label="耗时(ms)" width="110" align="right" />
        <el-table-column prop="totalTokens" label="Token" width="100" align="right" />
        <el-table-column prop="triggeredBy" label="触发人" width="120" />
        <el-table-column prop="createdAt" label="开始时间" width="200" />
        <el-table-column prop="finishedAt" label="结束时间" width="200" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onViewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="run-history__pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onRefresh"
          @current-change="onRefresh"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listAllWorkflowRuns, type WorkflowRunItem } from '@/api/workflow-runs'

const router = useRouter()

const loading = ref(false)
const records = ref<WorkflowRunItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const filterStatus = ref<string | undefined>(undefined)

async function loadData() {
  loading.value = true
  try {
    const res = await listAllWorkflowRuns(pageNum.value, pageSize.value, filterStatus.value)
    records.value = res.records
    total.value = res.total
  } catch (err: any) {
    ElMessage.error('加载执行记录失败: ' + (err?.message || err))
  } finally {
    loading.value = false
  }
}

function onRefresh() {
  loadData()
}

function onViewDetail(row: WorkflowRunItem) {
  router.push(`/workflow/runs/${row.runId}`)
}

function statusTagType(status: string) {
  switch (status) {
    case 'SUCCEEDED': return 'success'
    case 'FAILED': return 'danger'
    case 'RUNNING': return 'warning'
    case 'PAUSED': return 'info'
    default: return 'info'
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'SUCCEEDED': return '成功'
    case 'FAILED': return '失败'
    case 'RUNNING': return '运行中'
    case 'PAUSED': return '暂停'
    default: return status
  }
}

onMounted(loadData)
</script>

<style scoped>
.run-history__card {
  margin: 0;
}
.run-history__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.run-history__title {
  font-size: 16px;
  font-weight: 600;
}
.run-history__filter {
  display: flex;
  gap: 8px;
  align-items: center;
}
.run-history__pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
