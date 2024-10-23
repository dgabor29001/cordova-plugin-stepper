//
//  Pedometer.m
//  Copyright (c) 2014 Lee Crossley - http://ilee.co.uk
//

#import "Cordova/CDV.h"
#import "Cordova/CDVViewController.h"
#import "CoreMotion/CoreMotion.h"
#import "Stepper.h"

@interface Stepper ()
    @property (nonatomic, strong) CMPedometer *pedometer;
@end

@implementation Stepper

- (CMPedometer*) pedometer {
    if (_pedometer == nil) {
        _pedometer = [[CMPedometer alloc] init];
    }
    return _pedometer;
}

- (void) isStepCountingAvailable:(CDVInvokedUrlCommand*)command;
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[CMPedometer isStepCountingAvailable]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) isDistanceAvailable:(CDVInvokedUrlCommand*)command;
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[CMPedometer isDistanceAvailable]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) isFloorCountingAvailable:(CDVInvokedUrlCommand*)command;
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[CMPedometer isFloorCountingAvailable]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) startStepperUpdates:(CDVInvokedUrlCommand*)command;
{
    __block CDVPluginResult* pluginResult = nil;

    NSDictionary *options = [command.arguments objectAtIndex:0];
    NSString *timeZone = [options objectForKey:@"timeZone"];
    NSCalendar *calendar = [NSCalendar currentCalendar];
    if (timeZone && ![timeZone isKindOfClass:[NSNull class]] && [NSTimeZone timeZoneWithName:timeZone]) {
        calendar.timeZone = [NSTimeZone timeZoneWithName:timeZone];
    }
    NSDateComponents *dateComponents = [calendar components:NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay fromDate:[NSDate date]];
    NSDate *startDate = [calendar dateFromComponents:dateComponents];
    
    [self.pedometer startPedometerUpdatesFromDate:startDate withHandler:^(CMPedometerData *pedometerData, NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (error)
            {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
            }
            else
            {
                NSDictionary* pedestrianData = @{
                    @"startDate": [NSString stringWithFormat:@"%f", [pedometerData.startDate timeIntervalSince1970] * 1000],
                    @"endDate": [NSString stringWithFormat:@"%f", [pedometerData.endDate timeIntervalSince1970] * 1000],
                    @"steps_today": [CMPedometer isStepCountingAvailable] && pedometerData.numberOfSteps ? pedometerData.numberOfSteps : [NSNumber numberWithInt:0],
                    @"distance": [CMPedometer isDistanceAvailable] && pedometerData.distance ? pedometerData.distance : [NSNumber numberWithInt:0],
                    @"floorsAscended": [CMPedometer isFloorCountingAvailable] && pedometerData.floorsAscended ? pedometerData.floorsAscended : [NSNumber numberWithInt:0],
                    @"floorsDescended": [CMPedometer isFloorCountingAvailable] && pedometerData.floorsDescended ? pedometerData.floorsDescended : [NSNumber numberWithInt:0]
                };
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:pedestrianData];
                [pluginResult setKeepCallbackAsBool:true];
            }

            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        });
    }];
}

- (void) stopStepperUpdates:(CDVInvokedUrlCommand*)command;
{
    [self.pedometer stopPedometerUpdates];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) getStepsByPeriod:(CDVInvokedUrlCommand*)command;
{
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"];
    [dateFormatter setTimeZone:[NSTimeZone timeZoneForSecondsFromGMT:0]];

    NSDate* startDate = [dateFormatter dateFromString:[command.arguments objectAtIndex:0]];
    NSDate* endDate = [dateFormatter dateFromString:[command.arguments objectAtIndex:1]];

    __block CDVPluginResult* pluginResult = nil;

    [self.pedometer queryPedometerDataFromDate:startDate toDate:endDate withHandler:^(CMPedometerData *pedometerData, NSError *error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (error)
            {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
            }
            else
            {
                NSDictionary* pedestrianData = @{
                    @"steps": [CMPedometer isStepCountingAvailable] && pedometerData.numberOfSteps ? pedometerData.numberOfSteps : [NSNumber numberWithInt:0],
                    @"distance": [CMPedometer isDistanceAvailable] && pedometerData.distance ? pedometerData.distance : [NSNumber numberWithInt:0],
                    @"floorsAscended": [CMPedometer isFloorCountingAvailable] && pedometerData.floorsAscended ? pedometerData.floorsAscended : [NSNumber numberWithInt:0],
                    @"floorsDescended": [CMPedometer isFloorCountingAvailable] && pedometerData.floorsDescended ? pedometerData.floorsDescended : [NSNumber numberWithInt:0]
                };
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:pedestrianData];
            }

            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        });
    }];
}

- (void)getLastEntries:(CDVInvokedUrlCommand*)command {
    NSNumber *numberOfEntries = nil;
    NSDate *startDate = nil;
    NSDate *endDate = nil;
  
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"];
    [dateFormatter setTimeZone:[NSTimeZone timeZoneWithAbbreviation:@"UTC"]];
  
    NSCalendar *calendar = [NSCalendar currentCalendar];
    NSInteger x;
  
    // Check if arguments for startDate and endDate are provided
    if ([command.arguments count] > 1 && [command.arguments objectAtIndex:1] != [NSNull null]) {
        NSString *startDateString = [command.arguments objectAtIndex:1];
        startDate = [dateFormatter dateFromString:startDateString];
    }
    
    if ([command.arguments count] > 2 && [command.arguments objectAtIndex:2] != [NSNull null]) {
        NSString *endDateString = [command.arguments objectAtIndex:2];
        endDate = [dateFormatter dateFromString:endDateString];
    }
    
    // Check if numberOfEntries is provided
    if ([command.arguments count] > 0 && [command.arguments objectAtIndex:0] != [NSNull null]) {
        numberOfEntries = [command.arguments objectAtIndex:0];
    }

    // If startDate and endDate are not provided, calculate them based on numberOfEntries
    if (!startDate || !endDate) {
        if (![numberOfEntries isKindOfClass:[NSNumber class]]) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid argument. Please provide a valid number of entries."];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }

        x = [numberOfEntries integerValue];

        // Get the current date and time as the default endDate
        if (!endDate) {
            endDate = [NSDate date];
        }

        // Create a date components instance with the specified number of days
        NSDateComponents *dateComponents = [[NSDateComponents alloc] init];
        dateComponents.day = -x;  // Subtract 'x' days directly

        NSDate *calculatedStartDate = [calendar dateByAddingComponents:dateComponents toDate:endDate options:0];

        // Extract year, month, and day components to reset time to start of day
        NSDateComponents *calculatedStartDateComponents = [calendar components:(NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay) fromDate:calculatedStartDate];
        calculatedStartDateComponents.hour = 0;
        calculatedStartDateComponents.minute = 0;
        calculatedStartDateComponents.second = 0;

        // Set startDate to the calculated start of day
        startDate = [calendar dateFromComponents:calculatedStartDateComponents];
    } else {
      // Calculate the number of days between startDate and endDate
      NSDateComponents *components = [calendar components:NSCalendarUnitDay fromDate:startDate toDate:endDate options:0];
      x = components.day + 1;  // +1 to include both startDate and endDate
    }

    __block CDVPluginResult* pluginResult = nil;
    NSMutableArray *entriesArray = [NSMutableArray array];

    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    
    // Fetch pedometer data for each day starting from startDate until endDate
    for (NSInteger i = 0; i < x; i++) {
        [self.pedometer queryPedometerDataFromDate:startDate toDate:[startDate dateByAddingTimeInterval:24 * 60 * 60] withHandler:^(CMPedometerData *pedometerData, NSError *error) {
            dispatch_async(dispatch_get_main_queue(), ^{
                if (error) {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
                } else {
                    NSDictionary *pedestrianData = @{
                        @"data": [dateFormatter stringFromDate:pedometerData.startDate],
                        @"steps": [CMPedometer isStepCountingAvailable] && pedometerData.numberOfSteps ? pedometerData.numberOfSteps : [NSNumber numberWithInt:0],
                        @"distance": [CMPedometer isDistanceAvailable] && pedometerData.distance ? pedometerData.distance : [NSNumber numberWithInt:0]
                    };

                    [entriesArray addObject:pedestrianData];
                }

                if (i == x - 1) {
                    // If the last iteration, send the result
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{@"entries": entriesArray}];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                }
            });
        }];

        // Move the start date one day forward for the next iteration
        startDate = [startDate dateByAddingTimeInterval:24 * 60 * 60];
    }
}

@end
