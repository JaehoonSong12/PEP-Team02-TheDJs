package com.revature.todomanagement.cucumber.poms;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class RegistrationPom {
    private WebDriver driver;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "confirmPassword")
    private WebElement confirmPasswordInput;

    @FindBy(css = "[data-testid='error-message']")
    private WebElement statusMessage;

    public RegistrationPom(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public void enterCredentials(String username, String password) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        confirmPasswordInput.sendKeys(password);
    }

    public void enterCredentialsWithMismatch(String username, String password, String confirmPassword) {
        usernameInput.sendKeys(username);
        passwordInput.sendKeys(password);
        confirmPasswordInput.sendKeys(confirmPassword);
    }

    public String getStatusMessage() {
        return statusMessage.getText();
    }
}
