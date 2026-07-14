package com.revature.todomanagement.cucumber.steps;

import com.revature.todomanagement.cucumber.CucumberRunner;
import com.revature.todomanagement.cucumber.poms.RegistrationPom;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.By;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class RegistrationSteps {

    private WebDriver driver;
    private RegistrationPom registrationPom;

    private WebDriver getDriver() {
        if (driver == null) {
            driver = CucumberRunner.driver;
        }
        return driver;
    }

    @When("The user clicks the registration link")
    public void the_user_clicks_the_registration_link() {
        WebElement registerLink = getDriver().findElement(By.linkText("Register"));
        registerLink.click();
        // Wait for navigation to register page
        new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/register"));
        registrationPom = new RegistrationPom(getDriver());
    }

    @And("The user enters valid credentials")
    public void the_user_enters_valid_credentials() {
        String uniqueUsername = "u" + (System.currentTimeMillis() % 100000000000L);
        registrationPom.enterCredentials(uniqueUsername, "ValidPass1!");
    }

    @And("The user enters invalid credentials")
    public void the_user_enters_invalid_credentials() {
        // Enter data that passes client-side validation (non-empty, passwords match)
        // but fails server-side PasswordValidator (too short, no uppercase, no digit, no special char)
        registrationPom.enterCredentials("ab", "weak");
    }

    @And("The user clicks the register button")
    public void the_user_clicks_the_register_button() {
        getDriver().findElement(By.cssSelector("button[type='submit']")).click();
    }

    @Then("The user should be redirected to the login screen")
    public void the_user_should_be_redirected_to_the_login_screen() {
        new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/login"));
        assertTrue(getDriver().getCurrentUrl().contains("/login"));
    }

    @And("The user should remain on the registration page")
    public void the_user_should_remain_on_the_registration_page() {
        assertTrue(getDriver().getCurrentUrl().contains("/register"));
    }
}
