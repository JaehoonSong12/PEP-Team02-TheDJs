package com.revature.todomanagement.cucumber.steps;

import com.revature.todomanagement.cucumber.CucumberRunner;
import com.revature.todomanagement.cucumber.poms.LoginPom;
import com.revature.todomanagement.cucumber.poms.RegistrationPom;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class SharedSteps {

    private static final String BASE_URL = "http://localhost:4200";

    private WebDriver getDriver() {
        return CucumberRunner.driver;
    }

    @Given("The user is on the login page")
    public void the_user_is_on_the_login_page() {
        getDriver().get(BASE_URL + "/login");
    }

    @Then("The user should be given an error message")
    public void the_user_should_be_given_an_error_message() {
        WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(5));
        WebElement errorElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='error-message'], .error-message, .validation-error")));
        assertFalse(errorElement.getText().isEmpty(), "Error message should not be empty");
    }

    @Given("The user is logged in and on the dashboard")
    public void the_user_is_logged_in_and_on_the_dashboard() {
        WebDriver driver = getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // Generate a unique username for this test run
        String uniqueUsername = "u" + (System.currentTimeMillis() % 100000000000L);
        String password = "TestPass1!";

        // Step 1: Navigate to the login page
        driver.get(BASE_URL + "/login");

        // Step 2: Click the "Register" link to go to the registration page
        WebElement registerLink = driver.findElement(By.linkText("Register"));
        registerLink.click();

        // Step 3: Wait for URL to contain /register
        wait.until(ExpectedConditions.urlContains("/register"));

        // Step 4: Create a RegistrationPom and enter credentials
        RegistrationPom registrationPom = new RegistrationPom(driver);
        registrationPom.enterCredentials(uniqueUsername, password);

        // Step 5: Click the submit button to register
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Step 6: Wait for redirect back to /login
        wait.until(ExpectedConditions.urlContains("/login"));

        // Step 7: Create a LoginPom and enter the same credentials
        LoginPom loginPom = new LoginPom(driver);
        loginPom.enterCredentials(uniqueUsername, password);

        // Step 8: Click the submit button to log in
        loginPom.clickSubmitButton();

        // Step 9: Wait for URL to contain /dashboard
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }
}
