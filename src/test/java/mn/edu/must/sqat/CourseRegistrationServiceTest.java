package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "B242270801";
    private static final String STUDENT_EMAIL = "b242270801@must.edu.mn";
    private static final String COURSE_ID = "F.CSA313";
    private static final String PREREQUISITE = "CS201";

    @Mock
    private StudentRepository repository;

    @Mock
    private EmailSender emailSender;

    private CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private static Student student(boolean active, String... coursesTaken) {
        return new Student(
                STUDENT_ID,
                STUDENT_EMAIL,
                active,
                List.of(coursesTaken));
    }

    @Test
    @DisplayName("A1: Идэвхтэй оюутан урьдач нөхцөлөө хангасан үед OK буцаж, хадгалалт ба и-мэйл хийгдэнэ")
    void register_activeStudentWithPrerequisite_returnsOkAndSavesAndSendsEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, PREREQUISITE));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQUISITE));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(STUDENT_EMAIL), anyString(), anyString());
    }

    @Test
    @DisplayName("A2: Оюутан олдоогүй үед ERROR_NO_STUDENT буцаж, хадгалалт ба и-мэйл хийгдэхгүй")
    void register_missingStudent_returnsNoStudentWithoutSavingOrEmailing() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A3: Оюутан идэвхгүй үед ERROR_INACTIVE_STUDENT буцаж, и-мэйлтэй харьцахгүй")
    void register_inactiveStudent_returnsInactiveWithoutEmailInteractions() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(false, PREREQUISITE));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    @DisplayName("A4: Урьдач хичээл дутуу үед ERROR_PREREQUISITES буцаж, бүртгэл хадгалагдахгүй")
    void register_missingPrerequisite_returnsPrerequisitesWithoutSaving() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, "MATH101"));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQUISITE));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A5: И-мэйл илгээгч exception шидсэн ч OK буцаж, бүртгэл хадгалагдсан байна")
    void register_emailSenderThrows_stillReturnsOkAndSavesRegistration() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, PREREQUISITE));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQUISITE));
        doThrow(new RuntimeException("SMTP service is unavailable"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(STUDENT_EMAIL), anyString(), anyString());
    }

    @Test
    @DisplayName("A6: studentId эсвэл courseId null үед IllegalArgumentException шидэж, хамаарал дуудахгүй")
    void register_nullStudentOrCourseId_throwsIllegalArgumentExceptionWithoutInteractions() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(STUDENT_ID, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.register(null, null));

        verifyNoInteractions(repository, emailSender);
    }

    @Test
    @DisplayName("A7: Амжилттай бүртгэлийн и-мэйлийн биед хичээлийн код орсон байна")
    void register_successfulRegistration_emailBodyContainsCourseCode() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, PREREQUISITE));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQUISITE));
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(emailSender).send(
                eq(STUDENT_EMAIL),
                eq("Бүртгэл баталгаажлаа"),
                bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains(COURSE_ID),
                "И-мэйлийн биед courseId байх ёстой: " + bodyCaptor.getValue());
    }

    @ParameterizedTest(name = "A8[{index}]: studentExists={0}, active={1}, courseExists={2} → {3}")
    @CsvSource({
            "false, true,  true,  ERROR_NO_STUDENT",
            "true,  false, true,  ERROR_INACTIVE_STUDENT",
            "true,  true,  false, ERROR_NO_COURSE"
    })
    @DisplayName("A8: Оюутан, төлөв, хичээлийн гурван алдаанд харгалзах код буцааж хадгалалт хийхгүй")
    void register_errorScenario_returnsExpectedCodeWithoutSavingOrEmailing(
            boolean studentExists,
            boolean active,
            boolean courseExists,
            String expectedResult) {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(studentExists ? student(active, PREREQUISITE) : null);

        // A8-ийн мөр бүр register() дотор өөр салаанд буцдаг.
        // Тиймээс зарим мөрөнд ашиглагдахгүй stub-уудыг lenient болгов.
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(courseExists);
        lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQUISITE));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expectedResult, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }
}
