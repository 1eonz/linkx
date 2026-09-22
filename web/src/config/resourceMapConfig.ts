import { useI18n } from '@/hooks';

function config() {
  const { t } = useI18n();

  return {
    cameraCategoryType: '',
    categoryAbility: {},
    commonData: {},
    eventReportCategory: [],
    eventTargetInputTypeData: {},
    eventTargetMetaConfig: {},
    eventTargetMetaData: {},
    eventTargetMetaDataName: [],
    mapResource: {},
    quickReplyList: [
      t('resource.quickReply.replyOk'),
      t('resource.quickReply.replyBusy'),
      t('resource.quickReply.replyInconvenient'),
    ],
    resourceGroupComp: {},
    resourceGroupIntegrated: {},
    resourceIndividualAbility: {},
    resourceIndividualComp: {},
    resourceIndividualImg: {},
    resourceIndividualInfo: {},
    resourceIndividualIntegrated: {},
    resourceMapConfigData: [],
  };
}

export default config();
