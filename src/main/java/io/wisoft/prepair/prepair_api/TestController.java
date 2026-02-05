package io.wisoft.prepair.prepair_api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/hello")
    String getString() {
        return "Hello Test";
    }

    @GetMapping("/error")
    String triggerError() {
        throw new IllegalArgumentException("잘못된 요청 테스트");
    }
}
