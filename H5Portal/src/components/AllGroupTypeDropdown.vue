<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import caretDownSvg from '@/assets/svg/caret_down.svg';
	
type GroupType = '' | '1' | '2';
	
const props = defineProps<{
	modelValue: GroupType;
	active?: boolean;
}>();
	
const emit = defineEmits<{
	(e: 'update:modelValue', v: GroupType): void;
	(e: 'change', v: GroupType): void;
}>();
	
const show = ref(false);
	
const options: Array<{ label: string; value: GroupType }> = [
	{ label: '全部', value: '' },
	{ label: '协同群组', value: '2' },
	{ label: '普通群组', value: '1' },
];
	
const currentLabel = computed(() => {
	const hit = options.find((o) => o.value === props.modelValue);
	return hit?.label ?? '全部';
});
	
const actions = computed(() =>
	options.map((o) => ({
		text: o.label,
		value: o.value,
		className: o.value === props.modelValue ? 'is-selected' : '',
	})),
);
	
const onSelect = (action: any) => {
	const value = (action?.value ?? '') as GroupType;
	emit('update:modelValue', value);
	emit('change', value);
	show.value = false;
};
	
// 当外部变更 modelValue 时，保持弹层关闭（避免状态错乱）
watch(
	() => props.modelValue,
	() => {
		show.value = false;
	},
);
</script>
	
<template>
	<van-popover
		v-model:show="show"
		placement="bottom-start"
		trigger="click"
		:actions="actions"
		@select="onSelect"
		class="all-group-type-dropdown"
	>
		<template #reference>
			<view class="trigger" :class="{ active: props.active }">
				<text class="label">{{ currentLabel }}</text>
				<img class="caret" :src="caretDownSvg" alt="" />
			</view>
		</template>
	</van-popover>
</template>
	
<style lang="scss" scoped>
.trigger {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	gap: 6px;
	height: 24px;
	padding: 0 12px;
	margin-right: 12px;
	font-size: 14px;
	color: rgba(90, 99, 131, 1);
	background: rgba(245, 245, 245, 1);
	border-radius: 16px;
	box-sizing: border-box;
}
	
.trigger.active {
	background-color: #264ed1;
	color: #fff;
}
	
.caret {
	width: 10px;
	height: 10px;
}
</style>
	
<style lang="scss">
/* Popover 内容是 teleported 到 body，需要非 scoped 样式 */
.all-group-type-dropdown {
	.van-popover__content {
		padding: 0;
	}
	.van-popover__action-text{
		text-align: left;
		justify-content: left;
	}
	
	.van-popover__action {
		padding: 16px 40px 16px 16px;
		line-height: 54px;
		color: rgba(3, 11, 38, 1);
		font-size: 15px;
	}
	
	.van-popover__action.is-selected {
		color: #264ed1;
		background: rgba(38, 78, 209, 0.08);
	}
}
</style>
	