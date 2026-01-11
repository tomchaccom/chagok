import 'package:flutter/material.dart';
import 'package:chagok_flutter/screens/create_moment_screen.dart';
import 'package:chagok_flutter/screens/photo_orientation_screen.dart';
import 'package:chagok_flutter/screens/present_home_screen.dart';
import 'package:chagok_flutter/screens/past_screen.dart';
import 'package:chagok_flutter/screens/future_screen.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

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
      home: const HomeScaffold(),
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

class HomeScaffold extends StatefulWidget {
  const HomeScaffold({super.key});

  @override
  State<HomeScaffold> createState() => _HomeScaffoldState();
}

class _HomeScaffoldState extends State<HomeScaffold> {
  int _currentIndex = 1;

  final List<Widget> _tabs = const [
    PastScreen(),
    PresentHomeScreen(),
    FutureScreen(),
  ];

  void _onTabSelected(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _tabs,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _onTabSelected,
        selectedItemColor: AppColors.main,
        unselectedItemColor: AppColors.textSecondary,
        showUnselectedLabels: true,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.history_edu_outlined),
            label: '과거',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.auto_awesome_outlined),
            label: '현재',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.calendar_today_outlined),
            label: '미래',
          ),
        ],
      ),
    );
  }
}
