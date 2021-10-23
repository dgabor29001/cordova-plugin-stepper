var exec = require("cordova/exec");

var Stepper = function () {
    this.name = "Stepper";
};

// IOS & Android - Documented
Stepper.prototype.isStepCountingAvailable = function (onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "isStepCountingAvailable", []);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// IOS & Android - Documented
Stepper.prototype.startStepperUpdates = function (offset, onSuccess, onError, options) {
	if(typeof(onSuccess) === "object" && typeof(onError) == "undefined" && typeof(options) == "undefined") {
		options = onSuccess;
		onSuccess = undefined;
	}
    offset = parseInt(offset) || 0;
    options = options || {};
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "startStepperUpdates", [offset, options]);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// IOS & Android - Documented
Stepper.prototype.stopStepperUpdates = function (onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "stopStepperUpdates", []);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// Android - Not Available - UnDocumented
Stepper.prototype.getCurrentSteps = function (onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "getCurrentSteps", []);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// Android - Not Available - UnDocumented
Stepper.prototype.getDays = function (onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "getDays", []);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// Android - Not Available - UnDocumented
Stepper.prototype.getDaysWithoutToday = function (onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "getDaysWithoutToday", []);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// IOS & Android - Documented
Stepper.prototype.getSteps = function (date, onSuccess, onError) {
	const startDate = 1000*3600*24*Math.floor(new Date(date || new Date()).getTime()/(1000*3600*24));
	const endDate = 1000*3600*24*Math.ceil(new Date(date || new Date()).getTime()/(1000*3600*24)) - 1;
    return this.getStepsByPeriod(startDate, endDate, onSuccess, onError);
};

// Android - Behave wierd - Documented
Stepper.prototype.getStepsByPeriod = function (start, end, onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "getStepsByPeriod", [new Date(start || 0).toISOString(), new Date(end || 1000*3600*24*Math.ceil(new Date().getTime()/(1000*3600*24))).toISOString()]);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// Android - Not Available - UnDocumented
Stepper.prototype.getTotalWithoutToday = function (onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "getTotalWithoutToday", []);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// Android - Behave wierd - Documented
Stepper.prototype.getLastEntries = function (num, onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "getLastEntries", [num]);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// Android - Documented
Stepper.prototype.setNotificationLocalizedStrings = function (keyValueObj, onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "setNotificationLocalizedStrings", [keyValueObj]);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// Android - Documented
Stepper.prototype.setGoal = function (num, onSuccess, onError) {
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "setGoal", [num]);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

module.exports = new Stepper();