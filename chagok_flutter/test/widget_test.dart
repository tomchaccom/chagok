import 'package:flutter_test/flutter_test.dart';
import 'package:chagok_flutter/main.dart';

void main() {
  testWidgets('App smoke test', (tester) async {
    await tester.pumpWidget(const ChagokApp());
    expect(find.text('순간 기록하기'), findsOneWidget);
  });
}
