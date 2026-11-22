# گزارش تحویل Prompt 8

پیاده‌سازی روابط محلی روی شاخهٔ کاری موجود انجام شد. این گزارش قراردادهای تحویل‌شده و مرز اعتبار بررسی‌ها را ثبت می‌کند. فهرست دقیق فایل‌ها در انتهای سند آمده است.

## مسیرهای API

پیشوند همهٔ مسیرهای زیر `/api/master-data/local` است. سیزده خانوادهٔ عملیاتی، فهرست، جزئیات، ایجاد، PATCH و چهار عمل `activate`، `inactivate`، `delete` و `restore` دارند:

| خانواده | مسیر مجموعه |
| --- | --- |
| Context | `/organizations/{organizationId}/contexts` برای فهرست؛ `/organization-subprocess-scopes` برای ایجاد و `/organization-subprocess-scopes/{id}` برای جزئیات/تغییر |
| Control | `/organization-subprocess-scopes/{contextId}/control-scopes` |
| Risk | `/organization-subprocess-scopes/{contextId}/risk-scopes` |
| Objective | `/organization-subprocess-scopes/{contextId}/control-objective-scopes` |
| Requirement | `/organization-subprocess-scopes/{contextId}/requirement-scopes` |
| Risk–Control Coverage | `/organization-subprocess-scopes/{contextId}/risk-control-coverages` |
| Risk–Objective Coverage | `/organization-subprocess-scopes/{contextId}/risk-control-objective-coverages` |
| Control–Objective Coverage | `/organization-subprocess-scopes/{contextId}/control-control-objective-coverages` |
| Requirement–Control Coverage | `/organization-subprocess-scopes/{contextId}/requirement-control-coverages` |
| Organization Policy | `/organization-policy-scopes` |
| Context Policy | `/organization-subprocess-scopes/{contextId}/policy-scopes` |
| Control Policy | `/control-scopes/{localControlScopeId}/policy-scopes` |
| Requirement Policy | `/requirement-scopes/{localRequirementScopeId}/policy-scopes` |

`POST /control-assignments` انتساب اتمی کنترل است. گزینه‌های سراسری زیر `/options` شامل `organizations`، `subprocesses`، `controls`، `risk-templates`، `control-objectives`، `requirements`، `policies`، `control-owners`، `control-settings` و `central-control-scopes` هستند. چهار گزینهٔ Scope و چهار گزینهٔ Coverage در انتهای مسیر مجموعهٔ متناظر با `/options` ارائه می‌شوند. هفت نمای معکوس زیر `/central` برای Risk Template، Objective، Requirement و چهار Policy target ثبت شده‌اند. اسناد از API فعلی Document با ۱۳ target محلی استفاده می‌کنند.

## رابط کاربری

تب «زیرفرآیندها» Context را ویرایش می‌کند؛ «ریسک‌ها» Risk Scope و پوشش Risk–Objective را؛ «کنترل‌ها» Control Scope و سه پوشش Risk–Control، Control–Objective و Requirement–Control را؛ «مقررات» Requirement Scope را؛ «اهداف» Objective Scope را؛ و «سیاست‌ها» چهار هدف Policy را جداگانه. فهرست‌های مرتبط در تب‌های Risk/Regulations/Objectives فقط خواندنی‌اند و به ویرایشگر مالک هدایت می‌کنند. انتساب از صفحهٔ Central Subprocess و Central Control، و هفت نمای معکوس Central، به URL سازمان/زمینه/ردیف دقیق هدایت می‌شوند. ذخیرهٔ Local مستقل از ذخیرهٔ aggregate اطلاعات عمومی و اسناد سازمان است.

## نتیجهٔ A01–A18

| بند | نتیجه |
| --- | --- |
| A01 | ۱۳ entity/repository/DTO/API و managed type اضافه شد؛ تغییر schema وجود ندارد. |
| A02 | انتساب Central Subprocess فقط Context صریح ایجاد می‌کند و تب سازمان را باز می‌کند. |
| A03 | انتساب Control با Subprocess/Control صریح و ایجاد احتمالی Context در یک command پیاده شد. |
| A04 | Context غیرفعال/حذف‌شده و کلیدهای رزروشده بدون تغییر ضمنی چرخهٔ حیات رد می‌شوند. |
| A05 | یک LOCAL Revision با content مرتب و snapshot پیش/پس برای هر command در کد ساخته می‌شود؛ rollback واقعی اجرا نشده است. |
| A06 | هویت/source تغییرناپذیر، FK مشروط و تطابق Context/endpoint در سرویس‌ها بررسی می‌شوند. |
| A07 | ماتریس lifecycle، بازهٔ اعتبار، owner/code و خطاهای پایدار پیاده شد؛ پاسخ خطای Oracle در runtime اجرا نشده است. |
| A08 | Guardهای ثابت، قفل ردیف هدف و نسخه در همان تراکنش پیاده شدند؛ بررسی واقعی رقابت انجام نشده است. |
| A09 | مانع‌های حذف Local اضافه شد و منطق حذف Central دست‌نخورده ماند. |
| A10 | ماتریس Area در controller/service/options/reverse/Document اعمال شد؛ رفتار HTTP مجوزها در runtime اجرا نشده است. |
| A11 | شش تب سازمان با دادهٔ typed و دسترسی مستقل هر بخش فعال شدند؛ مرورگر واقعی اجرا نشده است. |
| A12 | مالکیت ویرایش Coverage، ارتباط Requirement و چهار هدف جداگانهٔ Policy پیاده شد. |
| A13 | deep link، انتخاب Context/row، وضعیت URL، guard پیش‌نویس و بازخورد تضاد اضافه شد؛ مرورگر واقعی اجرا نشده است. |
| A14 | ذخیرهٔ Local جدا از aggregate سازمان/اسناد آن است و نسخهٔ سازمان را نمی‌نویسد. |
| A15 | دو انتساب Central و هفت نمای معکوس به سازمان/Context/target/row هدایت می‌کنند. |
| A16 | ۱۳ target سند در resolver، authorization و مسیرهای Document اضافه شد؛ ذخیره‌سازی/دانلود واقعی اجرا نشده است. |
| A17 | کلیدهای فارسی/انگلیسی و کنترل‌های UI5 اضافه شد؛ lint/typecheck/build موفق‌اند، RTL و تعامل واقعی مرورگر اجرا نشده است. |
| A18 | تست، migration، قرارداد Central، خروجی tracked و قابلیت‌های خارج از دامنه تغییر نکرده‌اند. |

## قفل و تراکنش

هر command عمومی Local در `@Transactional` با propagation پیش‌فرض REQUIRED اجرا می‌شود؛ پس از مجوز، Guardهای ثابت به ترتیب نام enum گرفته می‌شوند، سپس ownership از شناسه‌های پایدار خوانده می‌شود و coordinator همان Guardها را در همان تراکنش برای یک LOCAL Revision دوباره می‌گیرد. در callback، قفل‌های هدف روی Organization، Subprocess، تعریف/Policy، مرجع Central لازم، Context، endpointهای Local و ردیف هدف گرفته می‌شوند؛ ترتیب دقیق هر خانواده در `lockReferences`/`lockOwned` همان service دیده می‌شود. snapshot قبل از mutation و بعد از flush در همان Revision ذخیره می‌شود.

| خانواده | Guardها به ترتیب |
| --- | --- |
| Context | ORGANIZATION, PROCESS |
| Control/assignment | CONTROL, ORGANIZATION, PROCESS |
| Risk | ORGANIZATION, PROCESS, RISK |
| Objective | ORGANIZATION, PROCESS |
| Requirement | ORGANIZATION, PROCESS, REGULATION |
| Risk–Control | CONTROL, ORGANIZATION, PROCESS, RISK |
| Risk–Objective | ORGANIZATION, PROCESS, RISK |
| Control–Objective | CONTROL, ORGANIZATION, PROCESS |
| Requirement–Control | CONTROL, ORGANIZATION, PROCESS, REGULATION |
| Organization Policy | ORGANIZATION, POLICY |
| Context Policy | ORGANIZATION, POLICY, PROCESS |
| Control Policy | CONTROL, ORGANIZATION, POLICY, PROCESS |
| Requirement Policy | ORGANIZATION, POLICY, PROCESS, REGULATION |

## بررسی و محدودیت

Backend با `-Dskip.ui=true -Dmaven.test.skip=true package` در JDK 21 داخل Docker و با کپی source به `/tmp/app` بسته‌بندی شد: موفق. `npm run lint` و `npm run build -- --outDir ../.codex-build/prompt-8-ui` موفق شدند. `git diff --check` نیز موفق بود. تست‌ها، startup روی Oracle، درخواست HTTP، ذخیره‌سازی Document و بررسی تراکنش‌های هم‌زمان بنا بر ممنوعیت تست در پرامپت اجرا نشدند. بستهٔ Backend با `skip.ui=true` UI تازه‌ساخته‌شده را در خود ندارد. استقرار Backend و UI باید با هم انجام شود؛ schema و migration تغییری ندارد.

## فهرست دقیق فایل‌های حاضر در تغییر

این فهرست در پایان کار از وضعیت Git تولید شده است؛ دو فایل `prompt-8-reviewed.md` و `prompt-8-review-fa.md` متن پرامپت/بازبینی قبلی‌اند و خروجی `.codex-build` محصول موقت build است.

- `grcone-app/src/main/java/com/digiaudit/grcone/common/api/ApiExceptionHandler.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/document/application/DocumentAuthorizationService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/document/application/DocumentCatalogPermissions.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/document/application/DocumentCommandService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/document/application/DocumentReadService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/document/infrastructure/target/JdbcDocumentTargetContextResolver.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/coverage/controlcontrolobjective/domain/repository/CentralSubprocessControlControlObjectiveCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/coverage/requirementcontrol/domain/repository/CentralSubprocessRequirementControlCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/coverage/riskcontrol/domain/repository/CentralSubprocessRiskControlCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/coverage/riskcontrolobjective/domain/repository/CentralSubprocessRiskControlObjectiveCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalContextDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalContextPolicyDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalControlDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalControlObjectiveCoverageDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalControlPolicyDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalObjectiveDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalOptionDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalOrganizationPolicyDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalRequirementControlCoverageDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalRequirementDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalRequirementPolicyDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalRiskControlCoverageDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalRiskDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/dto/LocalRiskObjectiveCoverageDtos.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalConstraintExceptionHandler.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalContextController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalContextPolicyController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalControlController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalControlObjectiveCoverageController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalControlPolicyController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalCoverageOptionsController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalObjectiveController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalOptionsController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalOrganizationPolicyController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalParsingExceptionHandler.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalReferenceOptionsController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalRequirementControlCoverageController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalRequirementController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalRequirementPolicyController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalReverseController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalRiskControlCoverageController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalRiskController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalRiskObjectiveCoverageController.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/api/LocalStrictBodyAdvice.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalCommandBadRequestException.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalCommandRules.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalContextPolicyService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalContextService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalControlObjectiveCoverageService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalControlPolicyService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalControlService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalObjectiveService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalOptionsService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalOrganizationPolicyService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalRequirementControlCoverageService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalRequirementHierarchy.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalRequirementPolicyService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalRequirementService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalReverseService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalRiskControlCoverageService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalRiskObjectiveCoverageService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/application/LocalRiskService.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalOrganizationSubprocessScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalPolicyControlScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalPolicyOrganizationScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalPolicyRequirementScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalPolicySubprocessScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessControlControlObjectiveCoverageEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessControlObjectiveScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessControlScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessRequirementControlCoverageEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessRequirementScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessRiskControlCoverageEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessRiskControlObjectiveCoverageEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/entity/LocalSubprocessRiskScopeEntity.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/LocalPropagationMode.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/LocalScopeAction.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/LocalSourceType.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalOrganizationSubprocessScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalPolicyControlScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalPolicyOrganizationScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalPolicyRequirementScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalPolicySubprocessScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessControlControlObjectiveCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessControlObjectiveScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessControlScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessRequirementControlCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessRequirementScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessRiskControlCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessRiskControlObjectiveCoverageRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/modules/masterdata/local/domain/repository/LocalSubprocessRiskScopeRepository.java`
- `grcone-app/src/main/java/com/digiaudit/grcone/runtime/GrconePersistenceConfiguration.java`
- `grcone-app/src/main/resources/messages_en.properties`
- `grcone-app/src/main/resources/messages_fa.properties`
- `grcone-app/src/main/resources/messages.properties`
- `grcone-docs/master-data/api-conventions.md`
- `grcone-docs/master-data/area-authorization.md`
- `grcone-docs/master-data/implementation-contract.md`
- `grcone-docs/master-data/prompt-8-delivery-report-fa.md`
- `grcone-docs/master-data/prompt-8-review-fa.md`
- `grcone-docs/master-data/prompt-8-reviewed.md`
- `grcone-ui/src/features/control-objective/pages/CentralControlObjectiveObjectPage.tsx`
- `grcone-ui/src/features/control/pages/CentralControlObjectPage.tsx`
- `grcone-ui/src/features/document/AGENTS.md`
- `grcone-ui/src/features/local-master-data/components/LocalContextAssignmentDialog.tsx`
- `grcone-ui/src/features/local-master-data/components/LocalControlAssignmentDialog.tsx`
- `grcone-ui/src/features/local-master-data/components/LocalLinkedCoverageList.tsx`
- `grcone-ui/src/features/local-master-data/components/LocalOptionPicker.tsx`
- `grcone-ui/src/features/local-master-data/components/LocalOrganizationWorkspace.tsx`
- `grcone-ui/src/features/local-master-data/components/LocalReverseList.tsx`
- `grcone-ui/src/features/local-master-data/components/LocalSectionPane.tsx`
- `grcone-ui/src/features/local-master-data/domain/local.model.ts`
- `grcone-ui/src/features/local-master-data/domain/local.sections.ts`
- `grcone-ui/src/features/local-master-data/index.ts`
- `grcone-ui/src/features/local-master-data/infra/local.api.repo.ts`
- `grcone-ui/src/features/organization/AGENTS.md`
- `grcone-ui/src/features/organization/pages/OrganizationObjectPage.tsx`
- `grcone-ui/src/features/organization/pages/OrganizationsFclShellPage.tsx`
- `grcone-ui/src/features/policy/pages/CentralPolicyObjectPage.tsx`
- `grcone-ui/src/features/process/pages/ProcessObjectPage.tsx`
- `grcone-ui/src/features/regulation/pages/CentralRegulationObjectPage.tsx`
- `grcone-ui/src/features/risk/pages/CentralRiskObjectPage.tsx`
- `grcone-ui/src/i18n/locales/en.json`
- `grcone-ui/src/i18n/locales/fa.json`
