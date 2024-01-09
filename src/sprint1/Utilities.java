package sprint1;

import java.util.Random;

public class Utilities {
  
  // https://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
  public static void shuffleArray(Object[] array) {
    int index;
    Object temp;
    Random random = new Random();
    for (int i = array.length - 1; i > 0; i--)
    {
        index = random.nextInt(i + 1);
        if (index != i)
        {
          index = random.nextInt(i + 1);
          temp = array[index];
          array[index] = array[i];
          array[i] = temp;
        }
    }
  }
}
