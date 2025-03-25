// This file defines a `LoanForm` widget which is a stateful widget
// that displays a loan application form.

import 'dart:math';

import 'package:flutter/material.dart';
import 'package:inbank_frontend/fonts.dart';
import 'package:inbank_frontend/widgets/national_id_field.dart';

import '../api_service.dart';
import '../colors.dart';

// LoanForm is a StatefulWidget that displays a loan application form.
class LoanForm extends StatefulWidget {
  const LoanForm({Key? key}) : super(key: key);

  @override
  _LoanFormState createState() => _LoanFormState();
}

class _LoanFormState extends State<LoanForm> {
  final _formKey = GlobalKey<FormState>();
  final _apiService = ApiService();
  String _nationalId = '';
  int _loanAmount = 2500;
  int _loanPeriod = 36;
  int _loanAmountResult = 0;
  int _loanPeriodResult = 0;
  int _suitableLoanPeriod = 0;
  String _errorMessage = '';

  // Submit the form and update the state with the loan decision results.
  // Only submits if the form inputs are validated.
  void _submitForm() async {
  if (_formKey.currentState!.validate()) {
    final result = await _apiService.requestLoanDecision(
        _nationalId, _loanAmount, _loanPeriod);

    setState(() {
      _loanAmountResult = int.parse(result['loanAmount'].toString());
      _loanPeriodResult = int.parse(result['loanPeriod'].toString());
      _errorMessage = result['errorMessage'].toString();
    });

    // If requested amount is too high, find a suitable loan period
    if (_loanAmount > _loanAmountResult) {
      int newLoanPeriod = _loanPeriodResult;

      while (_loanAmount > _loanAmountResult && newLoanPeriod < 60) {
        newLoanPeriod += 6; // Increment by 6 months
        if (newLoanPeriod > 60) break; // Prevent exceeding max allowed period

        final newResult = await _apiService.requestLoanDecision(
            _nationalId, _loanAmount, newLoanPeriod);

        int newAmountResult = int.parse(newResult['loanAmount'].toString());

        if (newAmountResult >= _loanAmount) {
          _suitableLoanPeriod = newLoanPeriod;
          break;
        }
      }

      // If no valid period is found, set _suitableLoanPeriod to 0
      if (_suitableLoanPeriod == 0) {
        _suitableLoanPeriod = 0;
      }
    } else {
      _suitableLoanPeriod = 0; // Hide if not needed
    }
  } else {
    setState(() {
      _loanAmountResult = 0;
      _loanPeriodResult = 0;
      _suitableLoanPeriod = 0;
    });
  }
}


  // Builds the application form widget.
  // The widget automatically queries the endpoint for the latest data
  // when a field is changed.
  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final formWidth = screenWidth / 3;
    const minWidth = 500.0;
    return Expanded(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          SizedBox(
            width: max(minWidth, formWidth),
            child: Form(
              key: _formKey,
              child: Column(
                children: [
                  FormField<String>(
                    builder: (state) {
                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          NationalIdTextFormField(
                            onChanged: (value) {
                              setState(() {
                                _nationalId = value ?? '';
                                _submitForm();
                              });
                            },
                          ),
                        ],
                      );
                    },
                  ),
                  const SizedBox(height: 60.0),
                  Text('Loan Amount: $_loanAmount €'),
                  const SizedBox(height: 8),
                  Slider.adaptive(
                    value: _loanAmount.toDouble(),
                    min: 2000,
                    max: 10000,
                    divisions: 80,
                    label: '$_loanAmount €',
                    activeColor: AppColors.secondaryColor,
                    onChanged: (double newValue) {
                      setState(() {
                        _loanAmount = ((newValue.floor() / 100).round() * 100);
                        _submitForm();
                      });
                    },
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: const [
                      Expanded(
                        child: Padding(
                          padding: EdgeInsets.only(left: 12),
                          child: Align(
                              alignment: Alignment.centerLeft,
                              child: Text('2000€')),
                        ),
                      ),
                      Expanded(
                        child: Padding(
                          padding: EdgeInsets.only(right: 12),
                          child: Align(
                            alignment: Alignment.centerRight,
                            child: Text('10000€'),
                          ),
                        ),
                      )
                    ],
                  ),
                  const SizedBox(height: 24.0),
                  Text('Loan Period: $_loanPeriod months'),
                  const SizedBox(height: 8),
                  Slider.adaptive(
                    value: _loanPeriod.toDouble(),
                    min: 12,
                    max: 60,
                    divisions: 40,
                    label: '$_loanPeriod months',
                    activeColor: AppColors.secondaryColor,
                    onChanged: (double newValue) {
                      setState(() {
                        _loanPeriod = ((newValue.floor() / 6).round() * 6);
                        _submitForm();
                      });
                    },
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: const [
                      Expanded(
                        child: Padding(
                          padding: EdgeInsets.only(left: 12),
                          child: Align(
                              alignment: Alignment.centerLeft,
                              child: Text('6 months')),
                        ),
                      ),
                      Expanded(
                        child: Padding(
                          padding: EdgeInsets.only(right: 12),
                          child: Align(
                            alignment: Alignment.centerRight,
                            child: Text('60 months'),
                          ),
                        ),
                      )
                    ],
                  ),
                  const SizedBox(height: 24.0),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16.0),
          Column(
            children: [
              Text(
                  'Approved maximum loan amount with a period of ${_loanPeriodResult != 0 ? _loanPeriodResult : "--"} months: ${_loanAmountResult != 0 ? _loanAmountResult : "--"} €'),
              const SizedBox(height: 8.0),
              Visibility(
                visible: _loanAmount > _loanAmountResult, // Show only if needed
                child: Text(
                  'Approved loan period with the desired amount of ${_loanAmount != 0 ? _loanAmount : "--"} €: ${_suitableLoanPeriod != 0 ? _suitableLoanPeriod : "--"} months'
                ),
              ),
              Visibility(
                  visible: _errorMessage != '',
                  child: Text(_errorMessage, style: errorMedium))
            ],
          ),
        ],
      ),
    );
  }
}
