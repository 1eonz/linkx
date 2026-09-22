<script lang="ts" setup>
  import { onMounted, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { flowMissionProcess } from '@/api/mission';
  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';
  import { getLang } from '@/locales';
  import { useMainStore, useMissionStore } from '@/store';

  const props = defineProps({
    warningData: {
      default: () => {},
      type: Object,
    },
  });
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const route = useRoute();

  const { flowConfig } = useMissionStore();
  const { updateWarningNoticeShowId } = useMainStore();

  const loading = ref(false);
  const formColumns = ref<any>([]);
  const action = 'INIT';

  watch(route, closeCard);

  onMounted(() => {
    initMissionForm();
  });

  // 初始化表单
  function initMissionForm() {
    const lang = getLang() === 'en' ? 'en' : 'zh';
    const ret: any = [];
    const { columns, i18nConf } = flowConfig.get('1');
    columns
      .find((i) => i.action === action)
      .definition.forEach((item) => {
        const { typeReference, web } = item.extensions;
        const { reference, type } = typeReference || {};
        const options: any = [];

        if (type === 'ENUM') {
          Object.keys(reference).forEach((key) => {
            options.push({
              label: i18nConf[lang][reference[key]],
              value: key,
            });
          });
        }

        const obj: any = {
          field: item.name,
          label: i18nConf[lang][item.i18n],
          options,
          show: type !== 'HIDE',
          type: type === 'ENUM' ? 'select' : 'input',
          value: '',
        };

        if (web.autoExpression) {
          const origin = { alarm: props.warningData };
          obj.value = `${getNestedValue(origin, web.autoExpression)}`;
        }

        if (item.name.toLowerCase().includes('time')) {
          obj.type = 'datePicker';
        }

        ret.push(obj);
      });

    formColumns.value = ret;
  }

  function closeCard() {
    emit('closeDialog');
  }

  // 确定将预警装换为警情任务
  async function deterMine() {
    loading.value = true;
    const { frObjectInfo, picture } = props.warningData;
    const param = {
      action,
      payload: {
        frObjectInfo,
        picture,
      },
    };

    unref(formColumns).forEach((item) => {
      if (item.value !== '') {
        param.payload[item.field] = item.value;
      }
    });

    const { code, msg } = await flowMissionProcess(1, param); // 任务处置
    if (code === 0) {
      Message(msg);
      updateWarningNoticeShowId('');
      closeCard();
    } else {
      Message({
        message: msg,
        type: 'error',
      });
    }
    loading.value = false;
  }

  // 通过路径字符串获取嵌套对象的值
  function getNestedValue(obj, path) {
    return path.split('.').reduce((acc, key) => {
      return acc && acc[key] !== undefined ? acc[key] : undefined;
    }, obj);
  }
</script>

<template>
  <!-- 创建警情 -->
  <div class="add-event-card">
    <TdFrameBox
      :dragger="true"
      size="small"
      :title="t('videoControl.createEvent.createIncident')"
      @close-frame-box="closeCard"
    >
      <div class="add-event-form">
        <div v-for="item in formColumns" :key="item.field" class="form-item">
          <template v-if="item.show">
            <div class="label">{{ item.label }}</div>
            <div class="content">
              <TdInput
                v-if="item.type === 'input'"
                v-model="item.value"
                :placeholder="t('common.search.inputContent')"
              />

              <ElSelect
                v-else-if="item.type === 'select'"
                v-model="item.value"
                :placeholder="t('common.tdcomp.choosePick')"
              >
                <ElOption
                  v-for="o in item.options"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </ElSelect>

              <ElDatePicker
                v-else
                v-model="item.value"
                :placeholder="t('videoControl.createEvent.enterIncidentTime')"
                prefix-icon="el-icon-tool"
                type="datetime"
              />
            </div>
          </template>
        </div>

        <div class="form-item form-submit">
          <TdButton
            class="td-button"
            :text="t('common.promptContent.cancel')"
            @click="closeCard()"
          />
          <TdButton
            class="td-button"
            :loading="loading"
            :text="t('common.promptContent.determine')"
            type="normal"
            @click="deterMine()"
          />
        </div>
      </div>
    </TdFrameBox>
  </div>
</template>

<style lang="less" scoped>
  .add-event-card {
    .add-event-form {
      display: flex;
      flex-direction: column;
      padding: 16px;

      .form-item {
        display: flex;
        margin-bottom: 10px;

        .label {
          width: 150px;
        }

        .content {
          flex: 1;
        }
      }

      .form-submit {
        display: flex;
        justify-content: center;
        width: 100%;

        .td-button {
          width: 140px;
          height: 36px;
          margin-right: 10px;
          text-align: center;
        }
      }
    }
  }

  :deep(.el-input) {
    .el-input__inner {
      padding-left: 10px;
    }
  }
</style>
