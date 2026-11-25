# Лабораторийн ажил №8 — Mockito mock объектууд

**Оюутан:** М.Наранбат  
**Оюутны код:** B242270801  
**Хичээл:** F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)

## 1. Зорилго

CourseRegistrationService-ийн StudentRepository болон EmailSender гэсэн хоёр гаднын хамаарлыг Mockito mock объектоор орлуулж, үйлчилгээний шийдвэр гаргалтыг нэгжийн түвшинд шалгав. Тестүүдэд when/thenReturn, verify/never, verifyNoInteractions, doThrow, ArgumentCaptor, @ParameterizedTest болон @CsvSource ашигласан.

## 2. Орчны мэдээлэл

Бодит гүйцэтгэлийн Java болон Maven хувилбар, эцсийн тестийн үр дүнг GitHub Actions ажиллуулсны дараа доор оруулна.

<!-- ENV-RESULTS-START -->

Энэ хэсэгт CI орчноос авсан java -version, mvn -version болон эцсийн тестийн бодит summary автоматаар нэмэгдэнэ. Бодит гаралт гарахаас өмнө зохиомол хувилбар, тестийн үр дүн оруулаагүй.

<!-- ENV-RESULTS-END -->

## 3. Төслийн бүтэц

- pom.xml — Java 17, JUnit Jupiter 5.10.2, Mockito JUnit Jupiter 5.11.0 болон Maven Surefire.
- src/main/java — Student record, StudentRepository интерфейс, EmailSender интерфейс, CourseRegistrationService.
- src/test/java — A1–A8 шалгалтуудыг агуулсан CourseRegistrationServiceTest.
- scripts — M1–M3 мутацийн туршилт болон README-д бодит үр дүн оруулах Python скрипт.
- results — Maven-ийн бодит гаралтын файлууд.

StudentRepository болон EmailSender-ийн бодит database, SMTP хэрэгжүүлэлт оруулаагүй. Эдгээрийг тест дотор mock объектоор орлуулсан.

## 4. Хэсэг A — Найман тест

MockitoExtension нь mock объектуудыг тест бүрийн өмнө автоматаар бэлтгэнэ. BeforeEach хэсэгт CourseRegistrationService-ийг хоёр mock dependency-тэй байгуулсан.

| № | Тестийн нэр | Шалгах зүйл |
|---|---|---|
| A1 | register_activeStudentWithPrerequisite_returnsOkAndSavesAndSendsEmail | Идэвхтэй оюутны бүртгэл OK болж хадгалагдан, и-мэйл илгээнэ. |
| A2 | register_missingStudent_returnsNoStudentWithoutSavingOrEmailing | Оюутан байхгүй үед ERROR_NO_STUDENT; хадгалалт болон и-мэйл байхгүй. |
| A3 | register_inactiveStudent_returnsInactiveWithoutEmailInteractions | Идэвхгүй оюутан ERROR_INACTIVE_STUDENT авч, EmailSender-тэй огт харьцахгүй. |
| A4 | register_missingPrerequisite_returnsPrerequisitesWithoutSaving | Урьдач хичээл дутуу үед ERROR_PREREQUISITES бөгөөд бүртгэл хадгалахгүй. |
| A5 | register_emailSenderThrows_stillReturnsOkAndSavesRegistration | EmailSender exception шидсэн ч бүртгэл хадгалагдаж OK буцна. |
| A6 | register_nullStudentOrCourseId_throwsIllegalArgumentExceptionWithoutInteractions | studentId, courseId тус бүр null болон хоёулаа null үед IllegalArgumentException шидэж, mock-уудтай харьцахгүй. |
| A7 | register_successfulRegistration_emailBodyContainsCourseCode | ArgumentCaptor ашиглан и-мэйлийн body дотор courseId байгааг шалгана. |
| A8 | register_errorScenario_returnsExpectedCodeWithoutSavingOrEmailing | @ParameterizedTest болон @CsvSource ашиглан NO_STUDENT, INACTIVE_STUDENT, NO_COURSE гэсэн гурван нөхцөлийг шалгана. |

Нийт 10 тестийн тохиолдол: долоон энгийн @Test болон A8-ийн гурван параметртэй мөр.

### A8 дээр lenient() ашигласан шалтгаан

A8-ийн параметр бүр register() дотор өөр өөр салаанд эрт буцдаг. Иймээс зарим мөрөнд courseExists() болон prerequisites() stub огт ашиглагдахгүй. Mockito-ийн strict stubs горимд ашиглагдаагүй stub нь unnecessary stubbing алдаа үүсгэж болох тул зөвхөн эдгээр хоёр нийтлэг stub дээр lenient() хэрэглэсэн. Бусад тестийн хатуу шалгалтыг сулруулаагүй.

## 5. Хэсэг B — Тестээ тестлэх (мутацийн туршилт)

CourseRegistrationService.java дээр дараах гурван зориудын алдааг тус тус оруулж, тест илрүүлж байгааг батална. Мутаци бүрийн дараа эх кодыг сэргээж, эцэст нь энгийн тестийг дахин ажиллуулна.

- M1: идэвхгүй оюутны active шалгалтыг авна. A3 болон A8-ийн идэвхгүй оюутны мөр илрүүлэх ёстой.
- M2: saveRegistration дуудалтыг авна. A1 болон A5 хадгалалт хийгдээгүйг илрүүлэх ёстой.
- M3: courseId null шалгалтыг авна. A6 дахь register(STUDENT_ID, null) тохиолдол exception шидэхээ болих тул мутантыг илрүүлнэ.

<!-- MUTATION-RESULTS-START -->

Энэ хэсгийн хүснэгтийг гурван мутантын бодит Maven log-оос автоматаар бөглөнө. Үр дүнг урьдчилан зохиож оруулаагүй.

<!-- MUTATION-RESULTS-END -->

Эцсийн ногоон туршилтад 10 тест, 0 failure, 0 error болон BUILD SUCCESS гарах ёстой. Эх код мутантын өөрчлөлтгүй, анхны хувилбараараа үлдэнэ.

## 6. Дүгнэлт

Mock болон fake хоёрын ялгаа нь mock-ийг тестийн явцад хүлээгдэх хариу, дуудалтаар тохируулдаг бол fake нь интерфейсийг хялбаршуулсан бодит хэрэгжүүлэлтээр орлуулдагт оршино. Энэ лабораторид repository-г mock болгосноор database сервергүйгээр бизнес логикийг шалгах боломжтой болсон. Харин Student record, String, List зэрэг энгийн өгөгдлийн объектыг mock хийх шаардлагагүй, жинхэнэ объектыг ашиглах нь ойлгомжтой. Ерөнхийдөө гаднын үйлчилгээ болон тестийн орчноос тусгаарлах шаардлагатай хамаарлыг mock хийх нь тохиромжтой. verify-г хэт олноор хэрэглэвэл тест нь үр дүнгээс илүү дотоод хэрэгжүүлэлтийн алхамд баригдаж, зөв рефакторинг хийсэн ч унах эрсдэлтэй. Гэхдээ хадгалалт, и-мэйл зэрэг буцах утгагүй гаж нөлөөг verify-ээр шалгах нь чухал. Иймээс үр дүнг assertion-аар, харин шаардлагатай гаж нөлөөг verify-ээр шалгах нь зөв гэж үзэв.

## 7. Ажиллуулах заавар

JDK 17 болон Maven суулгасны дараа төслийн үндсэн хавтсаас:

    mvn test

A1–A8 тест, M1–M3 мутаци, сэргээсэн эцсийн тестийг дарааллаар нь ажиллуулж бүх гаралтыг хадгалах:

    bash scripts/verify-lab.sh

## 8. GitHub Actions ба clean clone

.github/workflows/verify.yml нь main руу push хийх эсвэл workflow_dispatch-аар гараар эхлүүлэхэд JDK 17, Maven ашиглан тестүүдийг ажиллуулна. Workflow нь Java/Maven хувилбар, үндсэн тест, гурван мутантын log болон clean clone-оос дахин ажиллуулсан тестийн гаралтыг results хавтаст хадгална. Бүх шалгалт амжилттай бол README болон үр дүнгийн файлуудыг тусдаа CI commit болгон нэмнэ.

## 9. Git ажлын үе шат

Төслийн өөрчлөлтийг starter бүтэц, A1–A8 тест, мутацийн скрипт, GitHub Actions болон тайлан гэсэн ялгаатай үе шаттай commit-уудаар хадгалсан.
