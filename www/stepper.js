var exec = require("cordova/exec");

var Stepper = function () {
    this.name = "Stepper";
};

// IOS & Android - Documented
Stepper.prototype.isStepCountingAvailable = function (onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "isStepCountingAvailable", []);
};

// IOS & Android - Documented
Stepper.prototype.startStepperUpdates = function (offset, onSuccess, onError, options) {
    offset = parseInt(offset) || 0;
    options = options || {};
    exec(onSuccess, onError, "Stepper", "startStepperUpdates", [offset, options]);
};

// IOS & Android - Documented
Stepper.prototype.stopStepperUpdates = function (onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "stopStepperUpdates", []);
};

// IOS - UnDocumented
Stepper.prototype.queryData = function (onSuccess, onError, options) {
    exec(onSuccess, onError, "Stepper", "queryData", [options]);
};

// Android - UnDocumented
Stepper.prototype.getCurrentSteps = function (onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "getCurrentSteps", []);
};

// Android - Not Available - UnDocumented
Stepper.prototype.getDays = function (onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "getDays", []);
};

// Android - Not Available - UnDocumented
Stepper.prototype.getDaysWithoutToday = function (onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "getDaysWithoutToday", []);
};

// Android - Behave wierd - Documented
Stepper.prototype.getSteps = function (date, onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "getSteps", [date]);
};

// Android - Behave wierd - Documented
Stepper.prototype.getStepsByPeriod = function (start, end, onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "getStepsByPeriod", [start, end]);
};

// Android - Not Available - UnDocumented
Stepper.prototype.getTotalWithoutToday = function (onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "getTotalWithoutToday", []);
};

// Android - Behave wierd - Documented
Stepper.prototype.getLastEntries = function (num, onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "getLastEntries", [num]);
};

// Android - Documented
Stepper.prototype.setNotificationLocalizedStrings = function (keyValueObj, onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "setNotificationLocalizedStrings", [keyValueObj]);
};

// Android - Documented
Stepper.prototype.setGoal = function (num, onSuccess, onError) {
    exec(onSuccess, onError, "Stepper", "setGoal", [num]);
};

module.exports = new Stepper();