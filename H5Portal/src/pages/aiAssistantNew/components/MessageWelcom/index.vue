<!-- 综合模式: 首页默认 -->
 <template>
   <view class="init-robot">
     <text class="ai-title">👏你好，我是你的AI助手</text>
     <text class="ai-desc1">我可以帮你搜索、答疑、写作，请把你的任务</text>
     <text class="ai-desc2">交给我吧～</text>
   </view>
 </template>

 <style scoped lang="scss">
 /* 空默认显示 */
.init-robot {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  padding-top: 16.5vh;

  .ai-title {
    font-size: clamp(14px, 4.8vw, 18px);
    font-weight: 700;
    color: rgba(51, 51, 51, 1);
  }

  .ai-desc1 {
    margin-top: 2.7vh;
    font-size: clamp(12px, 4.2vw, 16px);
    color: rgba(90, 99, 131, 1);
  }

  .ai-desc2 {
    font-size: clamp(12px, 4.2vw, 16px);
    color: rgba(90, 99, 131, 1);
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {

    .ai-title,
    .ai-desc1,
    .ai-desc2 {
      font-size: 0.6rem;
    }
  }
}
</style>