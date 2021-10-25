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
Stepper.prototype.requestPermission = function (onSuccess, onError) {
	if(process.env.CORDOVA_PLATFORM !== "android") {
	  return onSuccess(true);
    }
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "requestPermission", []);
    });
    if (onSuccess) promise = promise.then(onSuccess);
    if (onError) promise = promise.catch(onError);
    return promise;
};

// IOS & Android - Documented
Stepper.prototype.startStepperUpdates = function (offset, onSuccess, onError, options) {
	let opts = options || {};
	if (typeof(offset) === "object") {
		opts = offset;
	} else {
		if (typeof(onSuccess) === "object") {
			opts = onSuccess;
			onSuccess = onError;
			onError = options;
		}
	    opts.offset = offset;
	}
    let promise = new Promise(function(resolve, reject) {
	    exec(resolve, reject, "Stepper", "startStepperUpdates", [opts]);
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
    return this.getStepsByPeriod(date || new Date(), date, onSuccess, onError);
};

// IOS & Android - Documented
Stepper.prototype.getStepsByPeriod = function (start, end, onSuccess, onError) {
	const startDate = new Date(start || 0);
	startDate.setHours(0, 0, 0, 0);
	const endDate = new Date(end || new Date());
	endDate.setHours(23, 59, 59, 999);
    let promise = new Promise(function(resolve, reject) {
        exec(resolve, reject, "Stepper", "getStepsByPeriod", [startDate.toISOString(), endDate.toISOString()]);
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