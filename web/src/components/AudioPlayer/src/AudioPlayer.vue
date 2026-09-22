<script lang="ts" setup>
  import { nextTick, onMounted, ref } from 'vue';

  import { useI18n } from '@/hooks';

  defineOptions({
    name: 'TdAudioPlayer',
  });

  const props = defineProps<{
    downloadable?: boolean;
    id: string;
    src: string;
  }>();

  const emit = defineEmits(['soundEnd']);

  const { t } = useI18n();
  const fileName = ref('');
  const draggableClasses = ref(['pin']);
  let currentlyDragged, currentTime, loading, player, playPauseBtn, progress, totalTime;

  onMounted(() => {
    const { id, src } = props;

    fileName.value = src.slice(Math.max(0, src.lastIndexOf('\\') + 1));
    const audioContent = document.getElementById(id);
    if (audioContent) {
      player = audioContent.querySelector('audio');
      playPauseBtn = audioContent.querySelector('.play-pause-btn');
      loading = audioContent.querySelector('.loading');
      totalTime = audioContent.querySelector('.controls__total-time');
      progress = audioContent.querySelector('.controls__progress');
      currentTime = audioContent.querySelector('.controls__current-time');
    }
    player.addEventListener('timeupdate', updateProgress.bind(this));
    player.addEventListener('loadedmetadata', () => {
      totalTime.textContent = formatTime(player.duration);
    });

    audioContent?.querySelector('.td-audio-player')?.addEventListener('mousedown', (event) => {
      event.stopPropagation(); // 防止拖拽事件冒泡
      if (isDraggable(event.target)) {
        currentlyDragged = event.target;
        const handleMethod = currentlyDragged.dataset.method;
        window.addEventListener('mousemove', handleMethod, false);
        window.addEventListener(
          'mouseup',
          () => {
            currentlyDragged = false;
            window.removeEventListener('mousemove', handleMethod, false);
          },
          false,
        );
      }
    });

    player.addEventListener('seeking', showLoadingIndicator);
    player.addEventListener('seeked', hideLoadingIndicator);
    player.addEventListener('canplay', hideLoadingIndicator);
    player.addEventListener('ended', () => {
      pausePlayer(player);
      player.currentTime = 0;
    });

    nextTick(() => {
      playPauseBtnClick();
    });
  });

  function isDraggable(el) {
    let canDrag = false;

    if (el.classList === undefined) return false; // fix for IE 11 not supporting classList on SVG elements

    for (let i = 0; i < draggableClasses.value.length; i++) {
      if (el.classList.contains(draggableClasses[i])) {
        canDrag = true;
      }
    }

    return canDrag;
  }

  function rewind(event) {
    if (inRange(event)) {
      player.currentTime = player.duration * getCoefficient(event);
    }
  }

  function inRange(event) {
    const touch = 'touches' in event; // instanceof TouchEvent may also be used
    const rangeBox = getRangeBox(event);
    const sliderPositionAndDimensions = rangeBox.getBoundingClientRect();
    const {
      dataset: { direction },
    } = rangeBox;
    let min = 0;
    let max = 0;

    if (direction === 'horizontal') {
      min = sliderPositionAndDimensions.x;
      max = min + sliderPositionAndDimensions.width;
      const clientX = touch ? event.touches[0].clientX : event.clientX;
      if (clientX < min || clientX > max) return false;
    } else {
      min = sliderPositionAndDimensions.top;
      max = min + sliderPositionAndDimensions.height;
      const clientY = touch ? event.touches[0].clientY : event.clientY;
      if (clientY < min || clientY > max) return false;
    }
    return true;
  }

  function getCoefficient(event) {
    const touch = 'touches' in event; // instanceof TouchEvent may also be used

    const slider = getRangeBox(event);
    const sliderPositionAndDimensions = slider.getBoundingClientRect();
    let K = 0;
    if (slider.dataset.direction === 'horizontal') {
      // if event is touch
      const clientX = touch ? event.touches[0].clientX : event.clientX;
      const offsetX = clientX - sliderPositionAndDimensions.left;
      const { width } = sliderPositionAndDimensions;
      K = offsetX / width;
    } else if (slider.dataset.direction === 'vertical') {
      const { height } = sliderPositionAndDimensions;
      const clientY = touch ? event.touches[0].clientY : event.clientY;
      const offsetY = clientY - sliderPositionAndDimensions.top;
      K = 1 - offsetY / height;
    }
    return K;
  }

  function getRangeBox(event) {
    let rangeBox = event.target;
    const el = currentlyDragged;
    if (event.type === 'click' && isDraggable(event.target)) {
      rangeBox = event.target.parentElement.parentElement;
    }
    if (event.type === 'mousemove') {
      rangeBox = el.parentElement.parentElement;
    }
    if (event.type === 'touchmove') {
      rangeBox = el.target.parentElement.parentElement;
    }
    return rangeBox;
  }

  function showLoadingIndicator() {
    playPauseBtn.style.display = 'none';
    loading.style.display = 'block';
  }

  function hideLoadingIndicator() {
    playPauseBtn.style.display = 'block';
    loading.style.display = 'none';
  }

  function updateProgress() {
    const current = player.currentTime;
    const percent = (current / player.duration) * 100;
    progress.style.width = `${percent}%`;
    if (progress.style.width === '100%') {
      emit('soundEnd', props.id);
    }
    currentTime.textContent = formatTime(current);
  }

  function playPauseBtnClick() {
    if (player.paused) {
      playPlayer(player);
    } else {
      pausePlayer(player);
    }
  }

  function pausePlayer(player) {
    const playPauseButton = player.parentElement.querySelector('.play-pause-btn__icon');
    playPauseButton.attributes.d.value = 'M12 8L0 16V0';
    player.pause();
  }

  function playPlayer(player) {
    const playPauseButton = player.parentElement.querySelector('.play-pause-btn__icon');
    playPauseButton.attributes.d.value = 'M0 0h4v16H0zM8 0h4v16h-4z';
    player.play();
  }

  function formatTime(time) {
    const min = Math.floor(time / 60);
    const sec = Math.floor(time % 60);
    return `${min}:${sec < 10 ? `0${sec}` : sec}`;
  }
</script>

<template>
  <div :id="id" class="td-audio-player other-td-audio-player">
    <div class="operation">
      <div class="loading">
        <div class="loading__spinner"></div>
      </div>

      <div class="play-pause-btn" @click="playPauseBtnClick">
        <svg height="16" viewBox="0 0 12 16" width="12" xmlns="http://www.w3.org/2000/svg">
          <path class="play-pause-btn__icon" d="M12 8L0 16V0" fill="#566574" fill-rule="evenodd" />
        </svg>
      </div>

      <TdTooltip :content="t('common.tdcomp.downloadAudio')">
        <div v-show="downloadable" class="download">
          <a class="download__link" :download="fileName" :href="src">
            <svg
              enable-background="new 0 0 29.978 29.978"
              fill="#566574"
              height="24"
              version="1.1"
              viewBox="0 0 29.978 29.978"
              width="24"
              xml:space="preserve"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="m25.462 19.105v6.848h-20.947v-6.848h-4.026v8.861c0 1.111 0.9 2.012 2.016 2.012h24.967c1.115 0 2.016-0.9 2.016-2.012v-8.861h-4.026z"
              />
              <path
                d="m14.62 18.426l-5.764-6.965s-0.877-0.828 0.074-0.828 3.248 0 3.248 0 0-0.557 0-1.416v-8.723s-0.129-0.494 0.615-0.494h4.572c0.536 0 0.524 0.416 0.524 0.416v8.742 1.266s1.842 0 2.998 0c1.154 0 0.285 0.867 0.285 0.867s-4.904 6.51-5.588 7.193c-0.492 0.495-0.964-0.058-0.964-0.058z"
              />
            </svg>
          </a>
        </div>
      </TdTooltip>
    </div>

    <div class="controls">
      <div>
        <span class="controls__current-time">0:00</span>
        /
        <span class="controls__total-time">0:00</span>
      </div>

      <!-- 感觉是未做完的东西，不展示 -->
      <div v-show="false" class="controls__slider slider" data-direction="horizontal">
        <div class="controls__progress gap-progress">
          <div class="pin progress__pin" :data-method="rewind"></div>
        </div>
      </div>
    </div>

    <audio>
      <source :src="src" type="audio/mpeg" />
    </audio>
  </div>
</template>

<style lang="less" scoped>
  .td-audio-player {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    user-select: none;
    background-color: transparent;
    border-radius: 4px;

    svg,
    img {
      display: flex;
    }

    .play-pause-btn {
      display: none;
      cursor: pointer;
    }

    .loading {
      height: 100%;

      .loading__spinner {
        width: 16px;
        height: 16px;
        border: 2px solid #b0b0b0;
        border-right-color: transparent;
        border-radius: 50%;
        animation: spin 0.4s linear infinite;
      }
    }

    .slider {
      position: relative;
      flex-grow: 1;
      background-color: #d8d8d8;

      .gap-progress {
        position: absolute;
        pointer-events: none;
        background-color: #44bfa3;
        border-radius: inherit;

        .pin {
          position: absolute;
          width: 16px;
          height: 16px;
          pointer-events: all;
          background-color: #44bfa3;
          border-radius: 8px;
          box-shadow: 0 1px 1px 0 rgb(0 0 0 / 32%);

          &::after {
            display: block;
            width: 200%;
            height: 200%;
            margin-top: -50%;
            margin-left: -50%;
            content: '';
            background: rgb(0 0 0 / 0%);
            border-radius: 50%;
          }
        }
      }
    }

    .controls {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      margin-right: 12px;
      margin-left: 12px;
      font-size: var(--font-size-default);
      line-height: 18px;
      color: #55606e;

      .controls__slider {
        width: 100px;
        height: 4px;
        margin-right: 16px;
        margin-left: 16px;
        border-radius: 2px;

        .controls__progress {
          width: 0;
          height: 100%;

          .progress__pin {
            top: -6px;
            right: -8px;
          }
        }
      }

      span {
        cursor: default;
      }
    }

    .download {
      margin-left: 50px;
      cursor: pointer;
    }
  }

  .event-td-audio-player {
    min-width: 15px;
    height: 90px;
  }

  .other-td-audio-player {
    min-width: 250px;
    height: 70px;
    box-shadow: 0 4px 16px 0 rgb(0 0 0 / 7%);
  }

  .operation {
    display: flex;
    flex-direction: row;
    justify-content: center;
    width: 100%;
    height: 100%;
    margin-top: 10px;
  }

  @keyframes spin {
    from {
      transform: rotateZ(0);
    }

    to {
      transform: rotateZ(1turn);
    }
  }
</style>
