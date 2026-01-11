import 'package:flutter/material.dart';
import 'package:chagok_flutter/screens/future_screen.dart';
import 'package:chagok_flutter/screens/past_screen.dart';
import 'package:chagok_flutter/screens/present_home_screen.dart';
import 'package:chagok_flutter/theme/app_theme.dart';

class MainNavigation extends StatefulWidget {
  const MainNavigation({super.key});

  @override
  State<MainNavigation> createState() => _MainNavigationState();
}

class _MainNavigationState extends State<MainNavigation> {
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
