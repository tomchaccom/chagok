import 'package:flutter/material.dart';
import 'package:chagok_flutter/screens/create_moment_screen.dart';
import 'package:chagok_flutter/screens/photo_orientation_screen.dart';
import 'package:chagok_flutter/theme/app_theme.dart';
import 'package:chagok_flutter/widgets/main_navigation.dart';

void main() {
  runApp(const ChagokApp());
}

class ChagokApp extends StatelessWidget {
  const ChagokApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Chagok',
      theme: buildAppTheme(),
      home: const MainNavigation(),
      routes: {
        CreateMomentScreen.routeName: (_) => const CreateMomentScreen(),
      },
      onGenerateRoute: (settings) {
        if (settings.name == PhotoOrientationScreen.routeName) {
          final args = settings.arguments as PhotoOrientationArguments;
          return MaterialPageRoute(
            builder: (_) => PhotoOrientationScreen(arguments: args),
          );
        }
        return null;
      },
    );
  }
}
