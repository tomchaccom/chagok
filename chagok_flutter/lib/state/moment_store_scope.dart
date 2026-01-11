import 'package:flutter/material.dart';
import 'package:chagok_flutter/state/moment_store.dart';

class MomentStoreScope extends InheritedNotifier<MomentStore> {
  const MomentStoreScope({
    super.key,
    required super.notifier,
    required super.child,
  });

  static MomentStore of(BuildContext context, {bool listen = true}) {
    if (listen) {
      final scope =
          context.dependOnInheritedWidgetOfExactType<MomentStoreScope>();
      assert(scope != null, 'MomentStoreScope not found in widget tree.');
      return scope!.notifier!;
    }
    final element =
        context.getElementForInheritedWidgetOfExactType<MomentStoreScope>();
    assert(element != null, 'MomentStoreScope not found in widget tree.');
    return (element!.widget as MomentStoreScope).notifier!;
  }
}

class MomentStoreProvider extends StatelessWidget {
  const MomentStoreProvider({
    super.key,
    required this.store,
    required this.child,
  });

  final MomentStore store;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return MomentStoreScope(
      notifier: store,
      child: child,
    );
  }
}

class MomentStoreConsumer extends StatelessWidget {
  const MomentStoreConsumer({super.key, required this.builder});

  final Widget Function(BuildContext context, MomentStore store) builder;

  @override
  Widget build(BuildContext context) {
    final store = MomentStoreScope.of(context);
    return builder(context, store);
  }
}

extension MomentStoreContext on BuildContext {
  MomentStore watchMomentStore() => MomentStoreScope.of(this);

  MomentStore readMomentStore() => MomentStoreScope.of(this, listen: false);
}
