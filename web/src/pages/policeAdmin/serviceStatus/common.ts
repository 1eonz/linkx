import { attendanceExport, queryAttendanceById } from '@/api/serviceStatus';
import { Dialog } from '@/components/Dialog';
import { useI18n } from '@/hooks';
import { queryPersonDetailById } from '@/pages/resource/resourceHelper';

import Details from './details.vue';

const { t } = useI18n();
// 状态
export const statusOptions = () => {
  return [
    {
      label: t('resource.resourceType.all'),
      value: '',
    },
    {
      label: t('resource.dutyStatus.idle'),
      value: 1,
    },
    {
      label: t('resource.dutyStatus.absences'),
      value: 2,
    },
    {
      label: t('resource.dutyStatus.busy'),
      value: 3,
    },
  ];
};

export function getStatusName(value) {
  let ret = '';

  statusOptions().forEach((item) => {
    if (item.value === value) {
      ret = item.label;
    }
  });

  return ret;
}

export const historyStatusOptions = [
  {
    label: t('resource.dutyStatus.attendance'),
    value: 1,
  },
  {
    label: t('resource.dutyStatus.unAttendance'),
    value: 2,
  },
];

/**
 * 查看详情
 * @param {string} id
 */
export async function lookDetails(id, row) {
  const person = await queryPersonDetailById(row.executorId);

  const { code, data } = await queryAttendanceById({ id });
  if (code === 0) {
    const cid = 'serviceStatusDetails';
    Dialog({
      cid,
      content: Details,
      data: { cid, info: data, person },
      shade: true,
    });
  }
}

/**
 * 导出
 */
export async function exportHandle(params) {
  const res = await attendanceExport(params);
  const excelBlob = new Blob([res.bolb]);
  const link = document.createElement('a');
  const filename = res['content-disposition'].split('filename=')[1];
  const name = decodeURI(filename);

  link.href = URL.createObjectURL(excelBlob);
  link.download = name;
  link.click();
}

export function timeStr(status) {
  const { t } = useI18n();
  const str = {
    1: t('resource.dutyStatus.dutyDate'),
    2: t('resource.dutyStatus.absencesDate'),
    3: t('resource.dutyStatus.busyDate'),
  };
  return str[status] || '';
}

export function addressStr(status) {
  const { t } = useI18n();
  const str = {
    1: t('resource.dutyStatus.dutyPosition'),
    2: t('resource.dutyStatus.absencesPosition'),
    3: t('resource.dutyStatus.busyPosition'),
  };
  return str[status] || '';
}
