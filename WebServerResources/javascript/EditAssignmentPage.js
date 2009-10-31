/**
 * Confirms that the user really wishes to remove a grading step from the
 * assignment.
 *
 * @param onYes the function to call that will invoke the remote action to
 *     remove the grading step
 */
removeGradingStep = function(onYes)
{
    webcat.confirm({
        title: "Confirm Remove",
        message: "Are you sure you want to remove this grading step? " +
            "All of its settings will be deleted. This operation cannot " +
            "be undone.",
        onYes: function() { onYes(); }
    });
};
