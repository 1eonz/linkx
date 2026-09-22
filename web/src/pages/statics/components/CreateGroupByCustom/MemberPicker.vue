<!-- MemberPicker.vue -->
<template>
  <div class="member-picker">
    <!-- Tab -->
    <div class="tab-bar">
      <span v-for="tab in TABS" :key="tab.value" class="tab-item" :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)">
        {{ tab.label }}
      </span>
    </div>
    <!-- 搜索 -->
    <div class="search-box">
      <i class="search-icon el-icon-search"></i>
      <input v-model="keyword" class="search-input" placeholder="请输入关键词" />
    </div>
    <!-- 节点切换（仅协同岗tab显示） -->
    <div v-if="isShow930 && activeTab === 'cooperated'" class="node-bar">
      <span
        v-for="(node, index) in nodeList"
        :key="index"
        class="node-item"
        :class="{ active: activeNodeIndex === index }"
        @click="switchNode(index)"
      >
        {{ node.name }}
      </span>
    </div>
    <!-- 面包屑（搜索时隐藏，按关注选时隐藏，非本节点隐藏，无层级时隐藏） -->
    <div v-if="!keyword && activeTab !== 'follow' && isLocalNode && !isCoopRootFallback" class="breadcrumb" ref="breadcrumbRef" @mousedown="startDrag" @mousemove="onDrag"
      @mouseup="endDrag" @mouseleave="endDrag" @wheel="onWheel">
      <span v-for="(crumb, index) in breadcrumbs" :key="crumb.id" class="crumb-item"
        :class="{ clickable: index < breadcrumbs.length - 1 }" @click="handleCrumbClick(index)">
        <template v-if="index < breadcrumbs.length - 1">
          <span class="crumb-text crumb-text-truncate" :title="crumb.name">{{ crumb.name }}</span><span
            class="crumb-split-icon"></span>
        </template>
        <template v-else><span class="crumb-text" :title="crumb.name">{{ crumb.name }}</span></template>
      </span>
    </div>

    <!-- 列表 -->
 <div class="list-container" ref="listRef" @scroll="onScroll">
      <!-- 初始加载中 -->
      <div
        v-if="loading && !currentMembers.length && !currentDepts.length"
        class="empty-tip"
      >
        加载中…
      </div>

      <!-- 搜索态 -->
      <template v-else-if="keyword">
        <!-- ✅ 使用虚拟列表渲染搜索结果 -->
        <RecycleScroller
          :items="activeTab === 'contacts' ? contactSearchList : activeTab === 'follow' ? followSearchList : coopSearchList"
          :item-size="56"
          key-field="id"
          :buffer="200"
          page-mode
        >
          <template #default="{ item: member }">
            <div class="list-item member-item" @click="emit('toggle', member)">
              <el-checkbox
                @click.stop
                :model-value="isSelected(member.id)"
                @change="emit('toggle', member)"
              />
              <img
                class="avatar"
                :src="getAvatarUrl(member.avatar || '')"
                alt=""
              />
              <span class="item-name">{{ member.name }}</span>
            </div>
          </template>
        </RecycleScroller>

        <!-- 加载提示 -->
        <div
          v-if="contactSearchLoadingMore || coopSearchLoadingMore || followSearchLoadingMore"
          class="empty-tip"
        >
          加载更多…
        </div>
        <div
          v-else-if="!(contactSearchHasMore || coopSearchHasMore || followSearchHasMore)"
          class="empty-tip"
        >
          已全部加载
        </div>
      </template>

      <!-- 层级态 -->
      <template v-else>
        <!-- 部门列表（数量通常不多，可以不用虚拟列表；非本节点不显示） -->
        <div
          v-if="isLocalNode"
          v-for="dept in currentDepts"
          :key="dept.id"
          class="list-item dept-item"
          @click="enterDept(dept)"
        >
          <span class="dept-name">{{ dept.name }}</span>
          <span class="arrow"></span>
        </div>

        <!-- ✅ 成员列表使用虚拟列表 -->
        <RecycleScroller
          v-if="currentMembers.length"
          :items="currentMembers"
          :item-size="56"
          key-field="id"
          :buffer="200"
          page-mode
        >
          <template #default="{ item: member }">
            <div class="list-item member-item" @click="emit('toggle', member)">
              <el-checkbox
                @click.stop
                :model-value="isSelected(member.id)"
                @change="emit('toggle', member)"
              />
              <img
                class="avatar"
                :src="getAvatarUrl(member.avatar || '')"
                alt=""
              />
              <span class="item-name">{{ member.name }}</span>
            </div>
          </template>
        </RecycleScroller>

        <!-- 加载提示 -->
        <div v-if="loadingMore || followLoadingMore" class="empty-tip">加载更多…</div>
        <div v-if="!hasMoreMembers && currentMembers.length && activeTab !== 'follow'" class="empty-tip">
          已全部加载
        </div>
        <div v-if="!followHasMore && currentMembers.length && activeTab === 'follow'" class="empty-tip">
          已全部加载
        </div>
        <div
          v-if="!loading && !currentDepts.length && !currentMembers.length"
          class="empty-tip"
        >
          暂无数据
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { Member, Department } from './types'
import { RecycleScroller } from "vue-virtual-scroller";
import "vue-virtual-scroller/dist/vue-virtual-scroller.css";
import { getIp } from '@/utils'
import {
  getCoopLevelChildren,
  getCoopLevelMembers,
  getAllUsers,
  getGroupList,
  getUsersPageOfType,
  getServers,
  getCoopUsersPage,
} from '@/api/collaboration'

// ── Props / Emits ─────────────────────────────────────────────
const props = defineProps<{
  selectedIds: string[]
  userId: number | string          // ✅ userId 是 number（接口要求）
}>()
const emit = defineEmits<{
  (e: 'toggle', member: Member): void
}>()

// ── 常量 ──────────────────────────────────────────────────────
const DEFAULT_AVATAR = 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png'
// 获取头像 URL：全路径直接使用，相对路径拼接 getIp()
const getAvatarUrl = (avatar: string) => {
  if (!avatar) return DEFAULT_AVATAR
  // http(s):// 开头视为全路径，直接使用
  if (/^https?:\/\//i.test(avatar)) {
    return avatar
  }
  return `${getIp()}/linkx/desktop${avatar}`
}
const TABS = [
  { label: '按组织选', value: 'contacts' },
  { label: '按关注选', value: 'follow' },
  { label: '协同岗', value: 'cooperated' },
] as const
const COOP_ROOT_ID = 0
const PAGE_SIZE = 200

// ── 层级状态 ──────────────────────────────────────────────────
const activeTab = ref<'contacts' | 'cooperated' | 'follow'>('cooperated')
const keyword = ref('')
// 分享协同岗特性主线版本下车，屏蔽前端入口
const isShow930 = ref(false)
// 节点列表（第一个为"本节点"）
const nodeList = ref<{ name: string; peerId: string }[]>([{ name: '本节点', peerId: '' }])
const activeNodeIndex = ref(0)
// 是否为本节点
const isLocalNode = computed(() => activeNodeIndex.value === 0)
const loading = ref(false)
const loadingMore = ref(false)
const breadcrumbs = ref<Department[]>([])
const currentNode = ref<Department | null>(null)
const listRef = ref<HTMLElement | null>(null)
const breadcrumbRef = ref<HTMLElement | null>(null)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragScrollLeft = ref(0)
const dragMoved = ref(false)
const coopPage = ref(1)
const hasMoreMembers = ref(true)
const allLoadedMembers = ref<Member[]>([])

// ── 协同岗根节点分页状态（层级数据为空时降级） ────────────────
const isCoopRootFallback = ref(false)  // 是否处于降级模式

// ── 通讯录层级状态 ────────────────────────────────────────────
const contactPage = ref(1)
const hasMoreContactMembers = ref(true)
const allLoadedContactMembers = ref<Member[]>([])
const contactLoadingMore = ref(false)

// ── 通讯录部门分页状态 ────────────────────────────────────────
const contactDeptPage = ref(1)
const hasMoreContactDepts = ref(true)
const contactDeptLoadingMore = ref(false)
const contactDeptTotal = ref(0)

// ── 通讯录搜索状态 ────────────────────────────────────────────
const contactSearchList = ref<Member[]>([])
const contactSearchPage = ref(1)
const contactSearchHasMore = ref(false)
const contactSearchLoading = ref(false)
const contactSearchLoadingMore = ref(false)

// ── 协同岗搜索状态 ────────────────────────────────────────────
const coopSearchList = ref<Member[]>([])
const coopSearchPage = ref(1)
const coopSearchHasMore = ref(false)
const coopSearchLoading = ref(false)
const coopSearchLoadingMore = ref(false)

// ── 按关注选列表状态 ──────────────────────────────────────────
const followList = ref<Member[]>([])
const followPage = ref(1)
const followHasMore = ref(true)
const followLoading = ref(false)
const followLoadingMore = ref(false)
const followTotal = ref(0)

// ── 按关注选搜索状态 ──────────────────────────────────────────
const followSearchList = ref<Member[]>([])
const followSearchPage = ref(1)
const followSearchHasMore = ref(false)
// const followSearchLoading = ref(false)
const followSearchLoadingMore = ref(false)

// ── 防抖 timer ────────────────────────────────────────────────
let searchTimer: ReturnType<typeof setTimeout> | null = null

// ── 计算属性 ──────────────────────────────────────────────────
const currentDepts = computed<Department[]>(() => currentNode.value?.children ?? [])
const currentMembers = computed<Member[]>(() => currentNode.value?.members ?? [])
const isSelected = (id: string) => props.selectedIds.includes(id)

// ── 数据映射 ──────────────────────────────────────────────────
// 协同岗部门映射（getCoopLevelChildren 返回格式）
function mapCoopDepts(raw: any[]): Department[] {
  return (raw ?? []).map(item => ({
    id: item.id ?? '',
    name: item.name ?? '',
    parentId: item.parentId ?? '',
    hasChildren: !!item.hasChildren,
    children: [],
    members: [],
    _loaded: false,
  }))
}
// 协同岗成员映射（getCoopLevelMembers 返回格式）
function mapCoopMembers(raw: any[]): Member[] {
  return (raw ?? []).map(item => ({
    id: item.id ?? '',
    name: item.name ?? '',
    avatar: item.iconUrl ?? '',
  }))
}
// 通讯录成员映射（getAllUsers 返回格式）
function mapContactMembers(raw: any[]): Member[] {
  return (raw ?? []).map(item => ({
    id: item.id ?? '',
    name: item.name ?? '',
    avatar: item.avatar ?? '',
  }))
}
// 通讯录部门映射（getGroupList 返回格式）
function mapContactDepts(raw: any[]): Department[] {
  return (raw ?? []).map(item => ({
    id: String(item.deptId ?? item.id),
    name: item.deptName ?? item.name ?? '',
    parentId: String(item.parentId ?? ''),
    children: [],
    members: [],
    _loaded: false,
  }))
}

// 关注列表成员映射（getUsersPageOfType 返回格式）
function mapFollowMembers(raw: any[]): Member[] {
  return (raw ?? []).map(item => ({
    id: item.friendId ?? item.id ?? '',
    name: item.name ?? '',
    avatar: item.avatar ?? '',
  }))
}
// ── Tab 切换 ──────────────────────────────────────────────────
async function switchTab(tab: 'contacts' | 'cooperated' | 'follow') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  keyword.value = ''
  breadcrumbs.value = []
  currentNode.value = null

  // 重置协同岗状态
  coopPage.value = 1
  hasMoreMembers.value = true
  allLoadedMembers.value = []

  // 重置协同岗根节点分页状态
  isCoopRootFallback.value = false

  // 重置分享协同岗状态
  sharedCoopPage.value = 1
  sharedCoopHasMore.value = true

  // 重置通讯录状态
  contactPage.value = 1
  hasMoreContactMembers.value = true
  allLoadedContactMembers.value = []

  // 重置通讯录部门分页状态
  contactDeptPage.value = 1
  hasMoreContactDepts.value = true
  contactDeptTotal.value = 0

  // 重置按关注选状态
  followList.value = []
  followPage.value = 1
  followHasMore.value = true
  followTotal.value = 0
  followSearchList.value = []
  followSearchPage.value = 1
  followSearchHasMore.value = false

  // 切换到协同岗tab时，根据当前节点决定加载方式
  if (tab === 'cooperated' && isShow930.value && !isLocalNode.value) {
    await loadSharedCoopList(1, true)
  } else {
    await loadRoot()
  }
}

// ── 加载根节点 ────────────────────────────────────────────────
async function loadRoot() {
  loading.value = true
  try {
    if (activeTab.value === 'cooperated') {
      const res = await getCoopLevelChildren(COOP_ROOT_ID)
      const children = mapCoopDepts((res as any)?.data ?? [])
      
      // 如果层级数据为空，降级到分页获取所有协同岗
      if (!children || children.length === 0) {
        isCoopRootFallback.value = true
        await loadSharedCoopList(1, true)
      } else {
        isCoopRootFallback.value = false
        currentNode.value = {
          id: 'root',
          name: res.data?.name ?? '协同岗',
          children,
          members: [],
          _loaded: true,
        }
      }
    } else if (activeTab.value === 'follow') {
      // 按关注选：直接加载关注列表
      await loadFollowList(1, true)
    } else {
      const res = await getGroupList(props.userId, { pageNo: 1, pageSize: PAGE_SIZE })
      const raw = (res as any)?.data ?? {}
      const children = mapContactDepts(raw.records ?? [])
      const total = Number(raw.total ?? 0)
      contactDeptTotal.value = total
      hasMoreContactDepts.value = children.length < total
      contactDeptPage.value = 1
      currentNode.value = {
        id: 'root',
        name: '按组织选',
        children,
        members: [],
        _loaded: true,
      }
    }
    breadcrumbs.value = currentNode.value ? [currentNode.value] : []
  } catch (e) {
    console.error('[loadRoot]', e)
  } finally {
    loading.value = false
  }
}

// ── 协同岗降级模式：分页获取所有协同岗 ─────────────────────────
// ── 分享协同岗：其他节点加载（type=2）────────────────────────
const sharedCoopPage = ref(1)
const sharedCoopHasMore = ref(true)
const sharedCoopLoadingMore = ref(false)
const sharedCoopTotal = ref(0)

function mapSharedCoopMembers(raw: any[]): Member[] {
  return (raw ?? []).map(item => ({
    id: String(item.coopUserId ?? ''),
    name: item.coopUserName ?? '',
    avatar: item.iconUrl ?? '',
  }))
}

async function loadSharedCoopList(page: number, reset = false) {
  if (sharedCoopLoadingMore.value && !reset) return
  if (reset) {
    loading.value = true
    sharedCoopLoadingMore.value = false
  } else {
    sharedCoopLoadingMore.value = true
  }

  try {
    const currentNodeData = nodeList.value[activeNodeIndex.value]
    const res = await getCoopUsersPage({
      peerId: isLocalNode.value ? '' : currentNodeData?.peerId,
      pageNum: page,
      pageSize: PAGE_SIZE,
      coopUserName: keyword.value || undefined,
    })
    const raw = (res as any)?.data ?? {}
    const list = mapSharedCoopMembers(raw.records ?? [])
    const total = Number(raw.total ?? 0)

    if (reset) {
      allLoadedMembers.value = list
      sharedCoopPage.value = 1
    } else {
      allLoadedMembers.value.push(...list)
      sharedCoopPage.value = page
    }

    sharedCoopTotal.value = total
    sharedCoopHasMore.value = allLoadedMembers.value.length < total

    currentNode.value = {
      id: 'root',
      name: currentNodeData?.name ?? '协同岗',
      children: [],
      members: [...allLoadedMembers.value],
      _loaded: true,
    }
    breadcrumbs.value = [currentNode.value]
  } catch (e) {
    console.error('[loadSharedCoopList]', e)
  } finally {
    loading.value = false
    sharedCoopLoadingMore.value = false
  }
}

// ── 加载节点列表 ──────────────────────────────────────────────
async function loadNodeList() {
  try {
    
    const res = await getServers({ pageNum: 1, pageSize: 100 })
    const raw = (res as any)?.data ?? res ?? {}
    const records = raw.records ?? raw ?? []
    nodeList.value = [
      { name: '本节点', peerId: '' },
      ...records.map((item: any) => ({
        name: item.name || item.peerId,
        peerId: item.peerId,
      })),
    ]
  } catch (e) {
    console.error('[loadNodeList]', e)
  }
}

// ── 切换节点 ──────────────────────────────────────────────────
async function switchNode(index: number) {
  if (activeNodeIndex.value === index) return
  activeNodeIndex.value = index
  keyword.value = ''
  breadcrumbs.value = []
  currentNode.value = null

  // 重置协同岗状态
  coopPage.value = 1
  hasMoreMembers.value = true
  allLoadedMembers.value = []
  isCoopRootFallback.value = false
  sharedCoopPage.value = 1
  sharedCoopHasMore.value = true

  if (isLocalNode.value) {
    // 本节点：保持现有逻辑
    await loadRoot()
  } else {
    // 其他节点：加载分享协同岗列表
    await loadSharedCoopList(1, true)
  }
}

// ── 按关注选：加载关注列表（分页）─────────────────────────────
async function loadFollowList(page: number, reset = false) {
  if (followLoadingMore.value && !reset) return

  if (reset) {
    followLoading.value = true
    followLoadingMore.value = false
  } else {
    followLoadingMore.value = true
  }

  try {
    const res = await getUsersPageOfType(props.userId, {
      pageNo: page,
      pageSize: PAGE_SIZE,
      userType: 1,
    })
    const raw = (res as any)?.data ?? {}
    const list = mapFollowMembers(raw.records ?? [])
    const total = Number(raw.total ?? 0)

    if (reset) {
      followList.value = list
    } else {
      followList.value.push(...list)
    }
    // 过滤掉自己
    followList.value = followList.value.filter(item => item.id !== String(props.userId))

    followPage.value = page
    followTotal.value = total
    followHasMore.value = followList.value.length < total

    // 更新 currentNode 用于渲染
    currentNode.value = {
      id: 'root',
      name: '我的关注',
      children: [],
      members: [...followList.value],
      _loaded: true,
    }
    breadcrumbs.value = currentNode.value ? [currentNode.value] : []
  } catch (e) {
    console.error('[loadFollowList]', e)
  } finally {
    followLoading.value = false
    followLoadingMore.value = false
  }
}

// ── 按关注选：搜索关注人（前端过滤，与H5一致） ──────────────
function searchFollow(kw: string) {
  // 接口搜索方式（暂不使用）
  // async function searchFollow(kw: string, page = 1) {
  //   if (page === 1) {
  //     followSearchLoading.value = true
  //     followSearchList.value = []
  //   } else {
  //     followSearchLoadingMore.value = true
  //   }
  //   try {
  //     const res = await getUsersPageOfType(props.userId, {
  //       pageNo: page,
  //       pageSize: PAGE_SIZE,
  //       keywords: kw,
  //       userType: 1,
  //     })
  //     const raw = (res as any)?.data ?? {}
  //     const list = mapFollowMembers(raw.records ?? [])
  //     const total = Number(raw.total ?? 0)
  //     if (page === 1) {
  //       followSearchList.value = list
  //     } else {
  //       followSearchList.value.push(...list)
  //     }
  //     followSearchList.value = followSearchList.value.filter(item => item.id !== String(props.userId))
  //     followSearchPage.value = page
  //     followSearchHasMore.value = followSearchList.value.length < total
  //   } catch (e) {
  //     console.error('[searchFollow]', e)
  //   } finally {
  //     followSearchLoading.value = false
  //     followSearchLoadingMore.value = false
  //   }
  // }

  // 前端过滤：从已加载的关注列表中按名称过滤
  followSearchList.value = followList.value.filter(item => item.name.includes(kw))
}

// 按关注选搜索加载更多（前端过滤模式下不需要）
// async function loadMoreFollowSearch() {
//   if (!followSearchHasMore.value || followSearchLoadingMore.value) return
//   await searchFollow(keyword.value, followSearchPage.value + 1)
// }


// ── 进入子部门 ────────────────────────────────────────────────
async function enterDept(dept: Department) {
  if (!dept._loaded) {
    await loadChildren(dept, true)
  }
  currentNode.value = dept
  breadcrumbs.value.push(dept)

  // 协同岗进入岗位时同步加载第一页成员
  if (activeTab.value === 'cooperated') {
    coopPage.value = 1
    hasMoreMembers.value = true
    allLoadedMembers.value = []
    await loadCoopMembers(dept, 1, true)
  } else {
    // 通讯录模式：重置部门分页状态
    contactDeptPage.value = 1
    hasMoreContactDepts.value = true
    
    // 加载该部门下的成员（分页）
    contactPage.value = 1
    hasMoreContactMembers.value = true
    allLoadedContactMembers.value = []
    await loadContactMembers(dept, 1, true)
  }

  // 滚动回顶
  if (listRef.value) listRef.value.scrollTop = 0
}

// ── 面包屑跳转 ────────────────────────────────────────────────
function jumpToCrumb(index: number) {
  if (index >= breadcrumbs.value.length - 1) return
  const target = breadcrumbs.value[index]
  breadcrumbs.value = breadcrumbs.value.slice(0, index + 1)
  currentNode.value = target
  
  // 重置协同岗状态
  coopPage.value = 1
  hasMoreMembers.value = true
  allLoadedMembers.value = target.members ?? []
  
  // 重置通讯录状态
  contactPage.value = 1
  hasMoreContactMembers.value = true
  allLoadedContactMembers.value = target.members ?? []
  
  // 重置通讯录部门分页状态
  contactDeptPage.value = 1
  hasMoreContactDepts.value = true
}

// ── 面包屑拖拽 ────────────────────────────────────────────────
function startDrag(e: MouseEvent) {
  if (!breadcrumbRef.value) return
  isDragging.value = true
  dragMoved.value = false
  dragStartX.value = e.pageX - breadcrumbRef.value.offsetLeft
  dragScrollLeft.value = breadcrumbRef.value.scrollLeft
  breadcrumbRef.value.style.cursor = 'grabbing'
  breadcrumbRef.value.style.userSelect = 'none'
}

function onDrag(e: MouseEvent) {
  if (!isDragging.value || !breadcrumbRef.value) return
  e.preventDefault()
  const x = e.pageX - breadcrumbRef.value.offsetLeft
  const walk = (x - dragStartX.value) * 1.5
  if (Math.abs(walk) > 3) {
    dragMoved.value = true
  }
  breadcrumbRef.value.scrollLeft = dragScrollLeft.value - walk
}

function endDrag() {
  if (!breadcrumbRef.value) return
  isDragging.value = false
  breadcrumbRef.value.style.cursor = 'grab'
  breadcrumbRef.value.style.userSelect = ''
}

function onWheel(e: WheelEvent) {
  if (!breadcrumbRef.value) return
  e.preventDefault()
  breadcrumbRef.value.scrollLeft += e.deltaY
}

function handleCrumbClick(index: number) {
  if (dragMoved.value) return
  jumpToCrumb(index)
}

// ── 加载子层级 ─────────────────────────────────────────
async function loadChildren(dept: Department, reset = false) {
  if (contactDeptLoadingMore.value && !reset) return
  
  if (reset) {
    contactDeptLoadingMore.value = false
  } else {
    contactDeptLoadingMore.value = true
  }
  
  try {
    if (activeTab.value === 'cooperated') {
      const res = await getCoopLevelChildren(dept.id)
      dept.children = mapCoopDepts((res as any)?.data ?? [])
    } else {
      const page = reset ? 1 : contactDeptPage.value + 1
      const res = await getGroupList(props.userId, {
        departmentId: dept.id === 'root' ? undefined : dept.id,
        pageNo: page,
        pageSize: PAGE_SIZE,
      })
      const raw = (res as any)?.data ?? {}
      const newDepts = mapContactDepts(raw.records ?? [])
      const total = Number(raw.total ?? 0)
      if (reset || raw?.current === 1) {
        dept.children = newDepts
        contactDeptPage.value = 1
      } else {
        dept.children = [...(dept.children ?? []), ...newDepts]
        contactDeptPage.value = page
      }
      
      contactDeptTotal.value = total
      hasMoreContactDepts.value = dept.children.length < total
      
      if (currentNode.value?.id === dept.id) {
        currentNode.value = { ...dept }
      }
    }
    dept._loaded = true
  } catch (e) {
    console.error('[loadChildren]', e)
  } finally {
    contactDeptLoadingMore.value = false
  }
}

// ── 加载协同岗成员（分页）────────────────────────────────────
async function loadCoopMembers(dept: Department, page: number, reset = false) {
  if (!hasMoreMembers.value && !reset) return
  if (reset) {
    loadingMore.value = false
  } else {
    loadingMore.value = true
  }

  try {
    // ✅ getCoopLevelMembers(levelId: number, params: { pageNum, pageSize })
    const res = await getCoopLevelMembers(dept.id, { pageNum: page, pageSize: PAGE_SIZE })
    const raw = (res as any)?.data ?? {}
    const list = mapCoopMembers(raw.records ?? [])
    const total = Number(raw.total ?? 0)

    if (reset) {
      allLoadedMembers.value = list
    } else {
      allLoadedMembers.value.push(...list)
    }
    // 选项列表过滤掉自己
    allLoadedMembers.value = allLoadedMembers.value?.filter(item => item.id !== props.userId)
    // 更新到 currentNode.members，触发计算属性更新
    dept.members = [...allLoadedMembers.value]
    if (currentNode.value?.id === dept.id) {
      currentNode.value = { ...dept }
    }

    hasMoreMembers.value = allLoadedMembers.value.length < total
    coopPage.value = page
  } catch (e) {
    console.error('[loadCoopMembers]', e)
  } finally {
    loadingMore.value = false
  }
}
// ── 加载通讯录成员（分页）────────────────────────────────────
async function loadContactMembers(dept: Department, page: number, reset = false) {
  if (!hasMoreContactMembers.value && !reset) return
  if (reset) {
    // 首次加载
    loading.value = true
    contactLoadingMore.value = false
  } else {
    contactLoadingMore.value = true
  }

  try {
    // ✅ getGroupList(userId, params: { departmentId, pageNo, pageSize })
    const res = await getAllUsers({
      departmentId: dept.id,
      pageNo: page,
      pageSize: PAGE_SIZE,
    })
    const raw = (res as any)?.data ?? {}
    
    // 处理成员列表
    const list = mapContactMembers(raw?.records ?? [])
    const total = Number(raw?.total ?? 0)

    if (reset) {
      allLoadedContactMembers.value = list
    } else {
      allLoadedContactMembers.value.push(...list)
    }
    // 选项列表过滤掉自己
    allLoadedContactMembers.value = allLoadedContactMembers.value.filter(item => item.id !== props.userId)
    // 更新到 currentNode 的 children 和 members，触发计算属性更新
    dept.members = [...allLoadedContactMembers.value]
    if (currentNode.value?.id === dept.id) {
      currentNode.value = { ...dept }
    }

    hasMoreContactMembers.value = allLoadedContactMembers.value.length < total
    contactPage.value = page
  } catch (e) {
    console.error('[loadContactMembers]', e)
  } finally {
    contactLoadingMore.value = false
    loading.value = false
  }
}

// ── 滚动加载更多（分页成员） ───────────────────────────
async function onScroll() {
  if (!listRef.value) return
  const el = listRef.value
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60

  if (!nearBottom) return

  if (keyword.value) {
    // 搜索态分页
    if (activeTab.value === 'contacts') {
      // 通讯录暂时没有分页功能，权限重构后恢复
      return 
      await loadMoreContactSearch()
    } else if (activeTab.value === 'follow') {
      // 前端过滤模式下不需要搜索分页加载更多
      // await loadMoreFollowSearch()
    } else {
      await loadMoreCoopSearch()
    }
    return
  }

  // 层级态：根据当前 tab 加载对应成员
  if (activeTab.value === 'cooperated' && currentNode.value && !loadingMore.value && !sharedCoopLoadingMore.value) {
    // 非本节点或本节点降级模式：加载分享协同岗列表
    if ((!isLocalNode.value || isCoopRootFallback.value) && sharedCoopHasMore.value) {
      await loadSharedCoopList(sharedCoopPage.value + 1)
    } else if (hasMoreMembers.value) {
      // 正常层级模式：加载成员
      await loadCoopMembers(currentNode.value, coopPage.value + 1)
    }
  } else if (activeTab.value === 'follow' && !followLoadingMore.value) {
    // 按关注选：加载更多关注人
    if (followHasMore.value) {
      await loadFollowList(followPage.value + 1)
    }
  } else if (activeTab.value === 'contacts' && currentNode.value && !contactLoadingMore.value && !contactDeptLoadingMore.value) {
    // 通讯录模式：先加载部门，部门加载完后再加载成员
    if (hasMoreContactDepts.value) {
      await loadChildren(currentNode.value, false)
    } else if (hasMoreContactMembers.value) {
      // await loadContactMembers(currentNode.value, contactPage.value + 1)
    }
  }
}

// ── 搜索：通讯录 ─────────────────────────────────────────────
async function searchContacts(kw: string, page = 1) {
  if (page === 1) {
    contactSearchLoading.value = true
    contactSearchList.value = []
  } else {
    contactSearchLoadingMore.value = true
  }

  try {
    // ✅ getAllUsers(params: { keywords?, pageNum?, pageSize? })
    const res = await getAllUsers({
      keywords: kw,
      pageNo: page,
      pageSize: PAGE_SIZE,
    })
    const raw = (res as any)?.data ?? {}
    const list = mapContactMembers(raw.records ?? [])
    const total = Number(raw.total ?? 0)

    if (page === 1) {
      contactSearchList.value = list
    } else {
      contactSearchList.value.push(...list)
    }
    console.log('contactSearchList.value, props.userId', contactSearchList.value, props.userId)
    // 选项列表过滤掉自己
    contactSearchList.value = contactSearchList.value.filter(item => item.id !== props.userId)
    contactSearchPage.value = page
    contactSearchHasMore.value = contactSearchList.value.length < total
  } catch (e) {
    console.error('[searchContacts]', e)
  } finally {
    contactSearchLoading.value = false
    contactSearchLoadingMore.value = false
  }
}

async function loadMoreContactSearch() {
  if (!contactSearchHasMore.value || contactSearchLoadingMore.value) return
  await searchContacts(keyword.value, contactSearchPage.value + 1)
}

// ── 搜索：协同岗位名 ─────────────────────────────────────────
async function searchCoopDutys(kw: string, page = 1) {
  if (page === 1) {
    coopSearchLoading.value = true
    coopSearchList.value = []
  } else {
    coopSearchLoadingMore.value = true
  }

  try {
    const currentNodeData = nodeList.value[activeNodeIndex.value]
    const res = await getCoopUsersPage({
      peerId: isLocalNode.value ? '' : currentNodeData?.peerId,
      pageNum: page,
      pageSize: PAGE_SIZE,
      coopUserName: kw,
    })
    const raw = (res as any)?.data ?? res ?? {}
    const list = mapSharedCoopMembers(raw.records ?? [])
    const total = Number(raw.total ?? 0)

    if (page === 1) {
      coopSearchList.value = list
    } else {
      coopSearchList.value.push(...list)
    }

    coopSearchPage.value = page
    coopSearchHasMore.value = coopSearchList.value.length < total
  } catch (e) {
    console.error('[searchCoopDutys]', e)
  } finally {
    coopSearchLoading.value = false
    coopSearchLoadingMore.value = false
  }
}

async function loadMoreCoopSearch() {
  if (!coopSearchHasMore.value || coopSearchLoadingMore.value) return
  await searchCoopDutys(keyword.value, coopSearchPage.value + 1)
}

// ── Watch：关键词变化触发搜索（防抖 300ms）──────────────────
watch(keyword, (kw) => {
  if (searchTimer) clearTimeout(searchTimer)

  if (!kw.trim()) {
    // 清空搜索，重置搜索状态
    contactSearchList.value = []
    contactSearchPage.value = 1
    contactSearchHasMore.value = false
    coopSearchList.value = []
    coopSearchPage.value = 1
    coopSearchHasMore.value = false
    followSearchList.value = []
    followSearchPage.value = 1
    followSearchHasMore.value = false
    return
  }

  searchTimer = setTimeout(() => {
    if (activeTab.value === 'contacts') {
      searchContacts(kw.trim())
    } else if (activeTab.value === 'follow') {
      searchFollow(kw.trim())
    } else {
      // 协同岗搜索（本节点和其他节点统一走 searchCoopDutys，内部按节点传 peerId）
      searchCoopDutys(kw.trim())
    }
  }, 300)
})

watch(activeTab, () => {
  if (breadcrumbs.value.length > 0 && breadcrumbs.value[0].id === 'root') {
    breadcrumbs.value[0].name =
      activeTab.value === 'cooperated' ? '协同岗位' : activeTab.value === 'follow' ? '我的关注' : '按组织选'
  }
})

// ── 初始化 ────────────────────────────────────────────────────
onMounted(async () => {
  await loadNodeList()
  loadRoot()
})
</script>



<style scoped lang="less">
.member-picker {
  display: flex;
  flex-direction: column;
  height: 100%;

  // ── Tab ────────────────────────────────────────────────
  .tab-bar {
    display: flex;
    border-bottom: 1px solid var(--border-color, #f0f0f0);

    .tab-item {
      flex: 1;
      text-align: center;
      padding: 10px 0;
      cursor: pointer;
      font-size: 16px;
      color: var(--tabs-color, #5a6383);
      position: relative;
      transition: color 0.2s;

      &.active {
        color: var(--tabs-active-color, #264ed1);
        font-weight: 600;

        &::after {
          content: '';
          position: absolute;
          bottom: -1px;
          left: 50%;
          transform: translateX(-50%);
          width: 80px;
          height: 2px;
          background: var(--tabs-active-color, #264ed1);
          border-radius: 1px;
        }
      }

      &:hover:not(.active) {
        color: var(--text-color, #333);
      }
    }
  }

  // ── 节点切换栏 ─────────────────────────────────────────
  .node-bar {
    display: flex;
    flex-wrap: nowrap;
    overflow-x: auto;
    border-bottom: 1px solid var(--border-color, #f0f0f0);

    &::-webkit-scrollbar {
      height: 0;
    }

    .node-item {
      position: relative;
      flex: 0 0 auto;
      padding: 8px 12px;
      cursor: pointer;
      font-size: 13px;
      color: var(--tabs-color, #5a6383);
      white-space: nowrap;
      transition: color 0.2s;

      &.active {
        color: var(--tabs-active-color, #264ed1);
        font-weight: 600;
        &::after {
          content: '';
          position: absolute;
          bottom: -1px;
          left: 50%;
          transform: translateX(-50%);
          width: 80%;
          height: 2px;
          background: var(--tabs-active-color, #264ed1);
          border-radius: 1px;
        }
      }

      &:hover:not(.active) {
        color: var(--text-color, #333);
      }
    }
  }

  // ── 搜索框 ─────────────────────────────────────────────
  .search-box {
    display: flex;
    align-items: center;
    margin: 12px 0 8px;
    padding: 7px 10px;
    background: rgba(0, 0, 0, 0.07);
    border-radius: 4px;
    gap: 6px;

    .search-icon {
      font-size: 14px;
      color: var(--tabs-color, #aaa);
    }

    .search-input {
      flex: 1;
      border: none;
      background: transparent;
      outline: none;
      font-size: 13px;
      color: var(--text-color, #333);

      &::placeholder {
        color: var(--tabs-color, #c0c0c0);
      }
    }
  }

  // ── 面包屑 ─────────────────────────────────────────────
  .breadcrumb {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 2px;
    min-height: 28px;
    margin: 8px 0;
    padding: 0 2px;
    overflow-x: auto;
    overflow-y: hidden;
    cursor: grab;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    .crumb-item {
      display: inline-flex;
      align-items: center;
      font-size: 13px;
      color: var(--text-color, #1d2129);
      white-space: nowrap;
      flex-shrink: 0;

      .crumb-text {
        display: inline-block;
        vertical-align: middle;

        &.crumb-text-truncate {
          max-width: 100px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      &.clickable {
        color: var(--tabs-active-color, #264ed1);
        cursor: pointer;

        &:hover {
          text-decoration: underline;
        }
      }
    }

    .crumb-split-icon {
      display: inline-block;
      width: 5px;
      height: 5px;
      border-top: 1.5px solid var(--tabs-color, #9a9ea8);
      border-right: 1.5px solid var(--tabs-color, #9a9ea8);
      transform: rotate(45deg) translateY(25%);
      transform-origin: center;
      margin: 0 5px;
      flex-shrink: 0;
    }
  }

  // ── 列表 ───────────────────────────────────────────────
  .list-container {
    flex: 1;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--border-color, #e0e0e0);
      border-radius: 4px;
    }

    .list-item {
      height: 56px;
      display: flex;
      align-items: center;
      padding: 7px 6px;
      border-radius: 4px;
      cursor: pointer;
      transition: background 0.15s;

      .dept-name {
        font-size: 14px;
        color: var(--text-color, #333);
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      &:hover {
        background: var(--list-item-hover-bg, #f5f7ff);
      }
    }

    // 部门行
    .dept-item {
      justify-content: space-between;

      .dept-name {
        font-size: 14px;
        color: var(--text-color, #333);
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .arrow {
        width: 6px;
        height: 6px;
        border-top: 1.5px solid var(--tabs-color, #bbb);
        border-right: 1.5px solid var(--tabs-color, #bbb);
        transform: rotate(45deg);
        flex-shrink: 0;
        position: relative;
        top: -1px;
      }
    }

    // 成员行
    .member-item {
      gap: 8px;

      // 覆盖 element-plus checkbox 默认样式
      :deep(.el-checkbox) {
        height: auto;
        margin-right: 0;

        .el-checkbox__inner {
          width: 15px;
          height: 15px;
          border-radius: 3px;
        }

        .el-checkbox__label {
          display: none;
        }
      }

      .avatar {
        width: 28px;
        height: 28px;
        border-radius: 6px;
        object-fit: cover;
        background: var(--is-blur-bg, #e8eaf6);
        display: block;
      }

      .item-name {
        flex: 1;
        font-size: 14px;
        color: var(--text-color, #333);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .empty-tip {
      text-align: center;
      padding: 30px 0;
      font-size: 13px;
      color: var(--tabs-color, #c0c0c0);
    }
  }
}
</style>
