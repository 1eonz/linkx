import { useI18n } from '@/hooks';

// mrs 类型
export const getCallTypeOptions = () => {
  const { t } = useI18n();
  return [
    {
      label: t('mrs.search.voiceCall'),
      value: '0',
    },
    {
      label: t('mrs.search.videoRecording'),
      value: '1',
    },
    {
      label: t('mrs.search.videoReturnRecording'),
      value: '2',
    },
    {
      label: t('mrs.search.groupCallRecordingVideo'),
      value: '3',
    },
    {
      label: t('mrs.search.cameraRecording'),
      value: '5',
    },
    {
      label: t('mrs.search.terminalSoundRecording'),
      value: '6',
    },
  ];
};
