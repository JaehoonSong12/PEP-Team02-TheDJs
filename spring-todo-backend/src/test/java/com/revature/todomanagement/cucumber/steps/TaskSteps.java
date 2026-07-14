package com.revature.todomanagement.cucumber.steps;

import com.revature.todomanagement.cucumber.CucumberRunner;
import com.revature.todomanagement.cucumber.poms.DashboardPom;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class TaskSteps {

    private DashboardPom dashboardPom;
    private int taskCountBeforeAction;

    private WebDriver getDriver() {
        return CucumberRunner.driver;
    }

    private DashboardPom getDashboardPom() {
        if (dashboardPom == null) {
            dashboardPom = new DashboardPom(getDriver());
        }
        return dashboardPom;
    }

    @When("The user enters {string} in the title input field")
    public void the_user_enters_in_the_title_input_field(String title) {
        getDashboardPom().enterTaskTitle(title);
    }

    @When("The user clicks the Add task button")
    public void the_user_clicks_the_add_task_button() {
        getDashboardPom().clickAddTaskButton();
    }

    @Then("The task {string} should appear in the task list")
    public void the_task_should_appear_in_the_task_list(String title) {
        assertTrue(getDashboardPom().isTaskVisible(title),
                "Task '" + title + "' should be visible in the task list");
    }

    @When("The user leaves the task input field empty")
    public void the_user_leaves_the_task_input_field_empty() {
        getDashboardPom().clearTaskInput();
        taskCountBeforeAction = getDashboardPom().getTaskCount();
    }

    @Then("No new task is added to the task list")
    public void no_new_task_is_added_to_the_task_list() {
        int currentTaskCount = getDashboardPom().getTaskCount();
        assertEquals(taskCountBeforeAction, currentTaskCount,
                "Task count should remain unchanged after attempting to add an empty task");
    }
}
