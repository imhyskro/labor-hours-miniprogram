import request, { type CommonResult } from '@/utils/request';

/** 导入错误行 */
export interface ImportErrorVO {
  row: number;
  studentId: string;
  reason: string;
}

/** 导入结果 */
export interface ImportResultVO {
  total: number;
  successCount: number;
  failCount: number;
  errors: ImportErrorVO[];
}

/** 导入学生（Excel 上传） */
export function importStudents(file: File): Promise<CommonResult<ImportResultVO>> {
  const formData = new FormData();
  formData.append('file', file);
  return request
    .post<CommonResult<ImportResultVO>>('/import/students', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data);
}

/** 导入助教（Excel 上传） */
export function importAssistants(file: File): Promise<CommonResult<ImportResultVO>> {
  const formData = new FormData();
  formData.append('file', file);
  return request
    .post<CommonResult<ImportResultVO>>('/import/assistants', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data);
}

/** 下载学生导入模板（返回二进制 Blob） */
export function downloadStudentTemplate(): Promise<Blob> {
  return request
    .get('/import/template/students', { responseType: 'blob' })
    .then((res) => res.data as Blob);
}

/** 下载助教导入模板（返回二进制 Blob） */
export function downloadAssistantTemplate(): Promise<Blob> {
  return request
    .get('/import/template/assistants', { responseType: 'blob' })
    .then((res) => res.data as Blob);
}
