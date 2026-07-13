package com.revature.todomanagement.cucumber.steps;

import com.revature.todomanagement.cucumber.CucumberRunner;
import com.revature.todomanagement.cucumber.poms.RegistrationPom;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
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

    private static final String BASE_URL = "http://localhost:4200";
    private WebDriver driver;
    private RegistrationPom registrationPom;

    private WebDriver getDriver() {
        if (driver == null) {
            driver = CucumberRunner.driver;
        }
        return driver;
    }

    @Given("The user is on the login page")
    public void the_user_is_on_the_login_page() {
        getDriver().get(BASE_URL + "/login");
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
        registrationPom.enterCredentials("testuser", "ValidPassword123");
    }

    @And("The user enters invalid credentials")
    public void the_user_enters_invalid_credentials() {
        // Empty credentials to trigger validation
        registrationPom.enterCredentials("", "");
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

    @Then("The user should be given an error message")
    public void the_user_should_be_given_an_error_message() {
        new WebDriverWait(getDriver(), Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("[data-testid='error-message']")));
        String message = registrationPom.getStatusMessage();
        assertFalse(message.isEmpty());
    }

    @And("The user should remain on the registration page")
    public void the_user_should_remain_on_the_registration_page() {
        assertTrue(getDriver().getCurrentUrl().contains("/register"));
    }
}
