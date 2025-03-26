package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;

/**
 * A service class that provides a method for calculating an approved loan
 * amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;
    private int estExpectedLifetime = 80;
    private int latExpectedLifetime = 85;
    private int litExpectedLifetime = 90;
    private int expectedLifetime = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their
     * ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @return A Decision object containing the approved loan amount and period, and
     *         an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is
     *                                      invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     * @throws NoValidLoanException         If there is no valid loan found for the
     *                                      given ID code, loan amount and loan
     *                                      period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        String country = getCountry(personalCode);

        if (country == "Estonia") {
            expectedLifetime = estExpectedLifetime - 5;
        } else if (country == "Latvia") {
            expectedLifetime = latExpectedLifetime - 5;
        } else if (country == "Lithuania") {
            expectedLifetime = litExpectedLifetime - 5;
        }

        int age = calculateAge(personalCode);

        if (age < 18 || age > expectedLifetime) {
            throw new NoValidLoanException("Loan was not given due to age restriction!");
        }

        int outputLoanAmount;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        while (highestValidLoanAmount(loanPeriod) < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            loanPeriod++;
        }

        if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT,
                    highestValidLoanAmount(loanPeriod));
        } else {
            throw new NoValidLoanException("No valid loan found!");
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    private int calculateAge(String personalCode) {
        if (personalCode == null || personalCode.length() != 11) {
            throw new IllegalArgumentException("Invalid Estonian personal code.");
        }

        int centuryIndicator = Character.getNumericValue(personalCode.charAt(0));
        int year = Integer.parseInt(personalCode.substring(1, 3));
        int month = Integer.parseInt(personalCode.substring(3, 5));
        int day = Integer.parseInt(personalCode.substring(5, 7));

        // Determine century based on the first digit
        int century;
        if (centuryIndicator == 1 || centuryIndicator == 2) {
            century = 1800;
        } else if (centuryIndicator == 3 || centuryIndicator == 4) {
            century = 1900;
        } else if (centuryIndicator == 5 || centuryIndicator == 6) {
            century = 2000;
        } else {
            throw new IllegalArgumentException("Invalid century indicator in personal code.");
        }

        int fullYear = century + year;
        LocalDate birthDate = LocalDate.of(fullYear, month, day);
        LocalDate today = LocalDate.now();

        return Period.between(birthDate, today).getYears();
    }

    private String getCountry(String perosnalCode) {
        // Get the last 4 digits of the ID
        String lastFourDigits = perosnalCode.substring(perosnalCode.length() - 4);
        int lastFour = Integer.parseInt(lastFourDigits);
        String originCountry = "";

        // Check country based on last 4 digits
        if (lastFour >= 0 && lastFour <= 3000) {
            originCountry = "Estonia";
        } else if (lastFour >= 3001 && lastFour <= 6000) {
            originCountry = "Latvia";
        } else if (lastFour >= 6001 && lastFour <= 9999) {
            originCountry = "Lithuania";
        }

        return originCountry;
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan
     * period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four
     * digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is
     *                                      invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }
}
