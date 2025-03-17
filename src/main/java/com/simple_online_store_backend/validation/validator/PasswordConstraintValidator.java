package com.simple_online_store_backend.validation.validator;

import com.simple_online_store_backend.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.passay.*;

import java.util.List;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        System.out.println("Password validation triggered for: " + password);

        PasswordValidator validator = new PasswordValidator(List.of(
                new LengthRule(8, 30),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new WhitespaceRule() //не должно быть пробелов
        ));

        RuleResult result = validator.validate(new PasswordData(password));

        if (result.isValid()) {
            return true;
        }

        context.disableDefaultConstraintViolation(); //Отключает стандартное сообщение об ошибке валидации.
        context.buildConstraintViolationWithTemplate(
                String.join(", ", validator.getMessages(result)) //Получает детальное сообщение из Passay
        ).addConstraintViolation(); //Добавляет сообщение об ошибке в контекст валидации

        return false;
    }
}
