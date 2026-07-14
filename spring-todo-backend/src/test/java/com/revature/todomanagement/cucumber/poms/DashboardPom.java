package com.revature.todomanagement.cucumber.poms;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DashboardPom {
    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(css = ".input-field[placeholder='Enter new task title...']")
    private WebElement taskTitleInput;

    @FindBy(css = ".task-list")
    private WebElement taskList;

    public DashboardPom(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void enterTaskTitle(String title) {
        taskTitleInput.clear();
        taskTitleInput.sendKeys(title);
    }

    public void clickAddTaskButton() {
        WebElement addTaskButton = driver.findElement(
                By.xpath("//button[contains(@class, 'btn-primary') and normalize-space(text())='Add Task']"));
        addTaskButton.click();
    }

    public boolean isTaskVisible(String title) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//ul[contains(@class, 'task-list')]//span[contains(@class, 'task-title') and normalize-space(text())='" + title + "']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getTaskCount() {
        List<WebElement> taskItems = driver.findElements(By.cssSelector(".task-list .task-item"));
        return taskItems.size();
    }

    public void expandTask(String taskTitle) {
        WebElement taskItem = driver.findElement(
                By.xpath("//ul[contains(@class, 'task-list')]//li[contains(@class, 'task-item')][.//span[contains(@class, 'task-title') and normalize-space(text())='" + taskTitle + "']]"));
        WebElement expandButton = taskItem.findElement(By.cssSelector(".btn-expand"));
        expandButton.click();
        // Wait for subtask section to appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//ul[contains(@class, 'task-list')]//li[contains(@class, 'task-item')][.//span[contains(@class, 'task-title') and normalize-space(text())='" + taskTitle + "']]//div[contains(@class, 'subtask-section')]")));
    }

    public void enterSubtaskTitle(String taskTitle, String subtaskTitle) {
        WebElement taskItem = driver.findElement(
                By.xpath("//ul[contains(@class, 'task-list')]//li[contains(@class, 'task-item')][.//span[contains(@class, 'task-title') and normalize-space(text())='" + taskTitle + "']]"));
        WebElement subtaskInput = taskItem.findElement(By.cssSelector(".subtask-input"));
        subtaskInput.clear();
        subtaskInput.sendKeys(subtaskTitle);
    }

    public void clickAddSubtaskButton(String taskTitle) {
        WebElement taskItem = driver.findElement(
                By.xpath("//ul[contains(@class, 'task-list')]//li[contains(@class, 'task-item')][.//span[contains(@class, 'task-title') and normalize-space(text())='" + taskTitle + "']]"));
        WebElement addSubtaskButton = taskItem.findElement(
                By.xpath(".//button[contains(@class, 'btn-primary') and normalize-space(text())='Add Subtask']"));
        addSubtaskButton.click();
    }

    public boolean isSubtaskVisible(String subtaskTitle, String parentTaskTitle) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//ul[contains(@class, 'task-list')]//li[contains(@class, 'task-item')][.//span[contains(@class, 'task-title') and normalize-space(text())='" + parentTaskTitle + "']]//ul[contains(@class, 'subtask-list')]//span[contains(@class, 'subtask-title') and normalize-space(text())='" + subtaskTitle + "']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getSubtaskCount(String parentTaskTitle) {
        WebElement taskItem = driver.findElement(
                By.xpath("//ul[contains(@class, 'task-list')]//li[contains(@class, 'task-item')][.//span[contains(@class, 'task-title') and normalize-space(text())='" + parentTaskTitle + "']]"));
        List<WebElement> subtaskItems = taskItem.findElements(By.cssSelector(".subtask-list .subtask-item"));
        return subtaskItems.size();
    }

    public String getErrorMessage() {
        try {
            WebElement errorElement = driver.findElement(By.cssSelector(".error-message"));
            return errorElement.getText();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    public void clearTaskInput() {
        taskTitleInput.clear();
    }

    public void clearSubtaskInput(String taskTitle) {
        WebElement taskItem = driver.findElement(
                By.xpath("//ul[contains(@class, 'task-list')]//li[contains(@class, 'task-item')][.//span[contains(@class, 'task-title') and normalize-space(text())='" + taskTitle + "']]"));
        WebElement subtaskInput = taskItem.findElement(By.cssSelector(".subtask-input"));
        subtaskInput.clear();
    }
}
