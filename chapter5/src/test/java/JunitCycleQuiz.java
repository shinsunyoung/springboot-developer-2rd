import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JunitCycleQuiz {

    @BeforeEach
    public void beforeEach() {
        System.out.println("Hello!");
    }


    @AfterAll
    public static void afterAll() {
        System.out.println("Bye!");
    }


    @Test
    public void junitQuiz3() {
        System.out.println("This is first test");
    }


    @Test
    public void junitQuiz4() {
        System.out.println("This is second test");
    }
}
