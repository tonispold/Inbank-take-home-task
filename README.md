# Inbank-take-home-task
Inbank Software Engineering Internship 2025 Take Home task

## TICKET-101 ##

This is a validation of TICKET-101 with highlights of how the code works and places for improvement.

### Tech stack: ###
**Backend:** Java Spring Boot with [estonian personal code validator](https://github.com/vladislavgoltjajev/java-personal-code)
<br>
**Frontend:** Flutter with Dart
<br>
**Database:** MySQL (MariaDB)
<br>
**API Integration:** RESTful API for communication between frontend and backend

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
Fixes to both of these shortcomings can be found here.
