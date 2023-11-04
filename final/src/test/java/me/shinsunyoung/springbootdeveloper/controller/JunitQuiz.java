package me.shinsunyoung.springbootdeveloper.controller;

import org.aspectj.lang.annotation.After;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;


public class JunitQuiz {

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

