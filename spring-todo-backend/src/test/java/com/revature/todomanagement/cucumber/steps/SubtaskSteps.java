package com.revature.todomanagement.cucumber.steps;

import com.revature.todomanagement.cucumber.CucumberRunner;
import com.revature.todomanagement.cucumber.poms.DashboardPom;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class SubtaskSteps {

    private DashboardPom dashboardPom;
    private String expandedTaskTitle;
    private int subtaskCountBeforeAction;

    private WebDriver getDriver() {
        return CucumberRunner.driver;
    }

    private DashboardPom getDashboardPom() {
        if (dashboardPom == null) {
            dashboardPom = new DashboardPom(getDriver());
        }
        return dashboardPom;
    }

    @And("A task {string} exists in the task list")
    public void a_task_exists_in_the_task_list(String title) {
        getDashboardPom().enterTaskTitle(title);
        getDashboardPom().clickAddTaskButton();
        assertTrue(getDashboardPom().isTaskVisible(title),
                "Task '" + title + "' should be visible in the task list after creation");
    }

    @When("The user expands the task {string}")
    public void the_user_expands_the_task(String title) {
        expandedTaskTitle = title;
        getDashboardPom().expandTask(title);
    }

    @And("The user enters {string} in the subtask title input field")
    public void the_user_enters_in_the_subtask_title_input_field(String subtaskTitle) {
        getDashboardPom().enterSubtaskTitle(expandedTaskTitle, subtaskTitle);
    }

    @And("The user clicks the Add subtask button")
    public void the_user_clicks_the_add_subtask_button() {
        getDashboardPom().clickAddSubtaskButton(expandedTaskTitle);
    }

    @Then("The subtask {string} should appear under {string}")
    public void the_subtask_should_appear_under(String subtaskTitle, String parentTaskTitle) {
        assertTrue(getDashboardPom().isSubtaskVisible(subtaskTitle, parentTaskTitle),
                "Subtask '" + subtaskTitle + "' should be visible under task '" + parentTaskTitle + "'");
    }

    @And("The user leaves the subtask title input field empty")
    public void the_user_leaves_the_subtask_title_input_field_empty() {
        getDashboardPom().clearSubtaskInput(expandedTaskTitle);
        subtaskCountBeforeAction = getDashboardPom().getSubtaskCount(expandedTaskTitle);
    }

    @Then("No new subtask is added under {string}")
    public void no_new_subtask_is_added_under(String parentTaskTitle) {
        int currentSubtaskCount = getDashboardPom().getSubtaskCount(parentTaskTitle);
        assertEquals(subtaskCountBeforeAction, currentSubtaskCount,
                "Subtask count should remain unchanged after attempting to add an empty subtask");
    }
}
