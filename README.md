# Inbank-take-home-task
Inbank Software Engineering Internship 2025 Take Home task

## Tech stack: ##
**Backend:** Java Spring Boot with [estonian personal code validator](https://github.com/vladislavgoltjajev/java-personal-code)
<br>
**Frontend:** Flutter with Dart
<br>
**Database:** MySQL (MariaDB)
<br>
**API Integration:** RESTful API for communication between frontend and backend

## How to run ##
Now to run the **backend**:
1. Navigate to the root directory of the backend.
2. Run `gradle build` to build the application.
3. Run `java -jar build/libs/inbank-backend-1.0.jar` to start the application

And to run the **frontend**:
1. Navigate to the root directory of the frontend.
2. Run `flutter pub get` to install the required dependencies.
3. Run `flutter run` to start the application in debug mode.

More information about **[frontend](https://github.com/deskrock/intern-decision-engine-frontend)** and **[backend](https://github.com/deskrock/intern-decision-engine-backend)**

## TICKET-101 ##

This is a validation of TICKET-101 with highlights of how the code works and places for improvement.

### 📜 Backend review ###
The backend logic consists of **4 important folders** with key files being:

- **DecisionEngineConstants.java**
<br>This holds all the necessary constants needed for the decision engine to make a decision based on user input.

- **DecisionEngineController.java**
<br>This is a REST controller handling requests for loan decisions.

- **DecisionRequest.java and DecisionResponse.java**
<br>DecisionRequest holds the data of the REST endpoint, which are the user inserted personal code, loan amount and period. DecisionResponse holds the data of the response, which consists of loan amount and period and an error message if needed to display.

- **Exceptions**
<br>There are **4 exceptions** implemented into the logic:
  - When the requested **loan amount** is invalid (`InvalidLoanAmountException.java`)
  - When the requested **loan period** is invalid (`InvalidLoanPeriodException.java`)
  - When the provided **personal ID code** is invalid (`InvalidPersonalCodeException.java`)
  - And when there is **no valid loan** found (`NoValidLoanException.java`)

- **Decision.java**
<br>This holds the final decision response data of the REST endpoint which consists of final loan amount and period and an error message if needed to display.

- **DecisionEngine.java**
<br>This is the main file of the backend where the decision values are calculated. It consists of **4 important functions:**
  - **verifyInputs** - checks if the user inserted values are according to parameters, if not, then returns a error message depending on the invalid user input
  - **getCreditModifier** - gets the credit modifier from the users personal id code with a very primitive method
  - **highestValidLoanAmount** - returns the largest valid loan amount based on the users credit modifier and chosen loan period
  - **calculateApprovedLoan** - returns the final loan that is approved based on the users credit modifier and chosen loan period and amount. If the users credit modifier equals to 0 (user is in debt), the functions returns a no valid loan exception. This also happens when the requested loan period is outside the allowed maximum loan period, which is declared in the DecisionEngineConstants.java file
  
The backend has **2 test files** which test the decision engine controller and decision engine. The tests use **mockito** to mock user inserted data.

### 📜 Frontend review ###
The frontend logic consists of **2 main files:**

- **api_service.dart**
<br>This file is responsible for making API requests to the backend. `requestLoanDecision()` method sends a **POST** request to `http://localhost:8080/loan/decision` with users personal code, loan amount and period. Then decodes the API response and updates the loan amount and period and the error messsage. Finally returns a Map with the loan details or an error message.

- **loan_form.dart**
<br>This file defines the LoanForm widget. The form allows users to input needed data and then automatically submits the form to get a loan decision. `_submitForm()` method sends a request to the backend using `_apiSerivce.requestLoanDecision()` from the `api_service.dart` file. Then the method updates the results based on API response.

### 💡 The biggest shortcomings of TICKET-101 and places for improvement ###
TICKET-101 has **2 big shortcomings:**

- If a suitable loan amount is not found within the selected period, the decision engine should try to find a new suitable period.
- Right now, the decision engine doesn't output the maximum sum that we would approve instantly. The task states: "The idea of the decision engine is to determine what would be the maximum sum, **regardless of the person requested loan amount**".

**Example:**

1. The users personal code is 48612065743. Her **credit modifier equals to 300** and she wants to loan **6700 EUR for 18 months**. Based on the logic and calculations credit score = `((300 / 6700) * 18) / 10 = 0.080...` and this result is less than 0.1, so we don't approve such a sum. Instead we approve the sum **credit modifier * loan period** so in this case `300 * 18 = 5400` But now the decision engine should output **a new suitable period** where the users loan would be approved. So we try adding a longer loan period and get `((300 / 6700) * 24) / 10 = 0.107...` and this result is over 0.1, so we approve this loan with the desired sum but with a new period.

2. Now lets say she wanted to loan **6700 EUR for 24 months** right from the beginning, then the maximum sum we would approve would be `300 * 24 = 7200`. Right now if the loan amount slider is at 6700 EUR the approved loan amount is also 6700 EUR. The user has to increase the loan amount slider to reach the maximum loan amount we would approve. In this case they need to increase it to 7300, to see that the maximum amount they can loan is 7200 EUR. This doesn't align with the requirements of the task.

Places for **improvement** for TICKET-101:

- At the moment, the **DecisionEngine** class performs multiple responsibilities like validating inputs, calculating the credit modifier and determining the loan amount and period.
  
  - **Improvement**: Extract input validation and credit modifier logic into separate service classes. This will improve maintainability and testability.
- The **DecisionEngine** directly instantiates EstonianPersonalCodeValidator, violating DIP (Dependency Inversion Principle).
  - **Improvement**: To inject EstonianPersonalCodeValidator as a dependency through the constructor, enabling easier testing and mock implementations.

### 🔧 Fixes to TICKET-101 ###
Code changes to both of these shortcomings can be found [here](https://github.com/tonispold/Inbank-take-home-task/commit/640c9b6825ab0b501c376f99478c734e70365dc6).

**How the new logic works?**

- The user sees the approved maximum loan amount regarding the chosen period. This is displayed all the time and changes accordingly with the change of loan period. (**Fix to shortcoming 1**)
- If the user now changes the loan amount and it scopes outside the maximum loan amount with the chosen period, then the approved loan period with the desired amount pops up. (**Fix to shortcoming 2**)
- If the user now changes the loan amount back inside the scope of maximum loan amount, then the approved loan period with the desired amount dissapears. It will be only displayed when needed.


## TICKET-102 ##

Implement age related restrictions to decision engine. The task states: "No loans should be given if the customer is underage or older than the current expected lifetime of each respectable country minus our maximum loan period." The task also states, that at first, only the baltic scope should be implemented and the personal code is in the same format in all countries and expected lifetimes can be arbitrary.

### 🔧 Implementation of TICKET-102 ###

For simplicity sake, a very primitive logic was added to the backend file `DecisionEngine.java`. Two functions were added:

- **calculateAge** - calculates the users age from the estonian id.
- **getCountry** - gets the users country from the estonian id. 0000-3000 is Estonia, 3001-6000 is Latvia and 6001-9999 is Lithuania.

You can see the **code changes** [here](https://github.com/tonispold/Inbank-take-home-task/commit/a69e6d8c10a0810a33eb2c1708e999ad19b4132a).
