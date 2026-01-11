import 'package:flutter/material.dart';

class AppColors {
  static const Color main = Color(0xFF5B5FEF);
  static const Color sub = Color(0xFFF4F1FF);
  static const Color textPrimary = Color(0xFF1B1B1F);
  static const Color textSecondary = Color(0xFF6F6F7A);
  static const Color border = Color(0xFFE3E0F5);
}

ThemeData buildAppTheme() {
  return ThemeData(
    useMaterial3: true,
    colorScheme: const ColorScheme.light(
      primary: AppColors.main,
      secondary: AppColors.main,
      surface: Colors.white,
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onSurface: AppColors.textPrimary,
    ),
    scaffoldBackgroundColor: AppColors.sub,
    textTheme: const TextTheme(
      headlineSmall: TextStyle(
        fontSize: 24,
        fontWeight: FontWeight.w600,
        color: AppColors.textPrimary,
      ),
      bodyMedium: TextStyle(
        fontSize: 14,
        color: AppColors.textPrimary,
      ),
      bodySmall: TextStyle(
        fontSize: 12,
        color: AppColors.textSecondary,
      ),
    ),
    snackBarTheme: const SnackBarThemeData(
      backgroundColor: AppColors.main,
      contentTextStyle: TextStyle(color: Colors.white),
    ),
  );
}
