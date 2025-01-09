import { useI18n } from 'vue-i18n';

class ReportReason {
  static FALSE_INFORMATION = new ReportReason('FALSE_INFORMATION');
  static INCOHERENCE = new ReportReason('INCOHERENCE');
  static PERSONAL_JUDGMENT = new ReportReason('PERSONAL_JUDGMENT');
  static OTHER = new ReportReason('OTHER');

  private constructor(public readonly key: string) {}

  shortLabel(): string {
    const { t } = useI18n();
    return t(`reportReason.${this.key}.short`);
  }
  longLabel(): string {
    const { t } = useI18n();
    return t(`reportReason.${this.key}.long`);
  }

  static values(): ReportReason[] {
    return [
      ReportReason.FALSE_INFORMATION,
      ReportReason.INCOHERENCE,
      ReportReason.PERSONAL_JUDGMENT,
      ReportReason.OTHER
    ];
  }
}

export default ReportReason;
