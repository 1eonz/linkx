
// 关联人员禁用状态
export const getUserDisable = (data, initUserIds) => {
    const { isBinding, id, hasPermission } = data
    if (initUserIds?.includes?.(id)) {
        return false
    }
    return isBinding === 1 || !hasPermission
}