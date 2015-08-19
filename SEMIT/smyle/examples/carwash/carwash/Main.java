package carwash;

    import drjava.smyle.*;

    class Main {
      public static void main(String[] args) {
        CarwashStore store = CarwashStore.open("store");

        Snapshot snapshot = store.mutableSnapshot();
        Table cars = store.cars(snapshot);

        // add a dirty Toyota
        cars.add(new Car().owner("John Doe").model("Toyota"));

        System.out.println("There are "+cars.count(new Car_filter().washedEquals(false))
          +" dirty cars in the carwash");

        // clean the Toyota
        cars.set(0, ((Car) cars.get(0)).washed(true));

        System.out.println("There are "+cars.count(new Car_filter().washedEquals(false))
          +" dirty cars in the carwash");

        snapshot.commit();

        store.close();
      }
    }
