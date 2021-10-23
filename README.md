# cordova-plugin-stepper

Lightweight pedometer Cordova/Phonegap plugin for Android using the hardware step sensor, with notifications.

Plugin using the hardware step-sensor for minimal battery consumption. This app is designed to be kept running all the time without having any impact on your battery life! Therefore the app does not drain any additional battery. Unlike other pedometer apps, this app does not track your movement or your location so it doesn't need to turn on your GPS sensor - no impact on your battery.

The plugin also creates a background service with a neat and nice notification (in Android platform) to continue working even after the application is closed and the device is restarted.

## Installation

#### Latest published version on npm (with Cordova CLI >= 5.0.0)

```
cordova plugin add cordova-plugin-stepper
```

#### Latest version from GitHub

```
cordova plugin add https://github.com/achubutkin/cordova-plugin-stepper
```
## Usage

#### startStepperUpdates (offset, options) => Promise 
Run with options and listener data updates. The success handler is called once during the first call and then called from the background thread whenever data is available.

The method also creates a background service with notification (Android only).

The `options` parameter may contain optional parameters. Below parameters recommended for notification localization (in Android platform):
- pedometerIsCountingText - _string_ - Set title text for notification
- pedometerStepsToGoFormatText - _string_ - Set description format string with text for notification
- pedometerYourProgressFormatText - _string_ - Set progress description format string with text for notification
- pedometerGoalReachedFormatText - _string_ - Set goal description format string with text for notification when the number of steps reaches the target value

Example:
```js
var offset = 0, options = { 
    pedometerIsCountingText: 'Pedometer is counting', 
    pedometerStepsToGoFormatText: '%s steps to go', // available variables: [stepsToGo, todaySteps, goal]. Insert using %1$s, %2$s, %3$s placeholders
    pedometerYourProgressFormatText: 'Your progress will be shown here soon', 
    pedometerGoalReachedFormatText: '%s steps today', // available variables: [todaySteps, goal]. Insert using %1$s, %2$s placeholders
  };
  
stepper
  .startStepperUpdates(offset, options)
  .then((result) => {
    var stepsToday = result.steps_today;
  })
  .catch((error) => {
    console.error(err);
  });
```

_Note: When the application is suspended, the call to handlers is temporarily suspended. When the application is closed, the background service continues to work (in Android platform). The background service continues after the device is restarted._

_To stop the background service, call the method `stopStepperUpdates`. When you open an application and call the launch method again, it joins the current background service._

#### stopStepperUpdates () => Promise 
The method stops the background calls to the success handler of the `startStepperUpdates` method and stops the background service (in Android platform) with remove notification.

Example:
```js
stepper.stopStepperUpdates()
  .then(() => {
    console.error("Stopped");
  })
  .catch((error) => {
    console.error(err);
  });
```

_Note: Background service can only be stopped by this method._

#### setGoal (num) => Promise
Set a goal (number of steps) for a pedometer.
When a goal is set, a progress bar is shown in the notification.

Example:
```js
var goal = 1000;

stepper.setGoal(goal)
  .then(() => {
    console.error("OK");
  })
  .catch((error) => {
    console.error(err);
  });
```

_Note: It is recommended to call the method before calling the method `startStepperUpdates`, but it is allowed to change the target during operation._

#### getSteps (date) => Promise
Gets the number of steps for the specified day. `date` parameter must be start of day and number of milliseconds since the Unix Epoch.

Example:
```js
var interval = 1000 * 60 * 60 * 24, 
  startOfDay = Math.floor(Date.now() / interval) * interval;

stepper.getSteps(startOfDay)
  .then((result) => {
    console.log(result.steps);
  })
  .catch((error) => {
    console.error(err);
  });
```

#### getStepsByPeriod (start, end) => Promise
Gets the number of steps for the specified period. `start` and `end` parameters must be start of day and number of milliseconds since the Unix Epoch.

Example:
```js
// 3 days period 
var interval = 1000 * 60 * 60 * 24, 
  start = Math.floor(Date.now() / interval) * interval - (interval * 3),
  end  = Math.floor(Date.now() / interval) * interval;

stepper.getSteps(start, end)
  .then((result) => {
    console.log(result.steps);
  })
  .catch((error) => {
    console.error(err);
  });
```

#### getLastEntries (num) => Promise
Gets all recent records in the specified limit.

Example:
```js
var limit = 10;

stepper.getLastEntries(limit)
  .then((result) => {
	  var entries = result.entries;
	  for (var i = 0; i < entries.length; i++) {
	    var entry = entries[i], data = entry.data,
	      steps = entry.steps;
	  }
  })
  .catch((error) => {
    console.error(err);
  });
```

## Platform and device support

- Android
- iOS

## Credits
Icons made by authors from https://www.flaticon.com is licensed by http://creativecommons.org/licenses/by/3.0/

## License

Copyright (c) 2021, Félicien François

Project based on source code and includes parts of source code https://github.com/achubutkin/cordova-plugin-stepper
Copyright (c) 2019, Alexandr Chubutkin

Project based on source code and includes parts of source code https://github.com/j4velin/Pedometer 
Copyright (c) 2013 Thomas Hoffmann - All Rights Reserved

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.