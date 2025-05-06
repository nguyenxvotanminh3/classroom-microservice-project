Lập trình hướng hàm (Functional Programming) trong Java là một phong cách lập trình trong đó các hàm là các công dân hạng nhất, nghĩa là chúng có thể được gán cho biến, truyền làm tham số hoặc trả về như một kết quả của hàm khác. Java 8 đã giới thiệu nhiều tính năng để hỗ trợ lập trình hướng hàm, chẳng hạn như lambda expressions, Stream API và các interface chức năng (functional interfaces).

### Lợi ích của Lập trình Hướng hàm

1. **Dễ bảo trì và mở rộng:** Các hàm nhỏ, độc lập dễ kiểm tra, gỡ lỗi và bảo trì.
2. **Code ngắn gọn và rõ ràng:** Lambda expressions và các phương thức trong Stream API giúp viết code ngắn gọn và dễ hiểu hơn.
3. **Tránh trạng thái biến đổi:** Hạn chế các biến trạng thái giúp giảm lỗi và cải thiện khả năng dự đoán.
4. **Đơn giản hóa xử lý song song:** Stream API giúp xử lý dữ liệu song song một cách dễ dàng và hiệu quả hơn.

### Các Tính năng trong Java hỗ trợ Lập trình Hướng hàm

#### 1. Lambda Expressions
Lambda expressions giúp định nghĩa các hàm một cách ngắn gọn.

```java
// Ví dụ cơ bản với lambda expression
List<String> names = Arrays.asList("John", "Jane", "Jack");
names.forEach(name -> System.out.println(name));
```

#### 2. Functional Interfaces
Functional Interfaces là các interface có chính xác một phương thức trừu tượng. Một số functional interface phổ biến trong Java là `Function`, `Predicate`, `Consumer`, `Supplier`.

```java
// Ví dụ sử dụng Function Interface
Function<String, Integer> stringLength = s -> s.length();
int length = stringLength.apply("Hello");
System.out.println(length); // Output: 5
```

#### 3. Stream API
Stream API cung cấp các phương thức để xử lý dữ liệu theo phong cách hướng hàm.

```java
// Ví dụ sử dụng Stream API
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
List<Integer> squares = numbers.stream()
                               .map(n -> n * n)
                               .collect(Collectors.toList());
System.out.println(squares); // Output: [1, 4, 9, 16, 25]
```

### Các Tình huống Thường gặp

#### 1. Lọc dữ liệu
Sử dụng `filter` để lọc các phần tử trong một danh sách.

```java
List<String> names = Arrays.asList("John", "Jane", "Jack", "Jill");
List<String> filteredNames = names.stream()
                                  .filter(name -> name.startsWith("J"))
                                  .collect(Collectors.toList());
System.out.println(filteredNames); // Output: [John, Jane, Jack, Jill]
```

#### 2. Biến đổi Dữ liệu
Sử dụng `map` để biến đổi các phần tử trong danh sách.

```java
List<String> names = Arrays.asList("John", "Jane", "Jack");
List<Integer> nameLengths = names.stream()
                                 .map(String::length)
                                 .collect(Collectors.toList());
System.out.println(nameLengths); // Output: [4, 4, 4]
```

#### 3. Tổng hợp Dữ liệu
Sử dụng `reduce` để tổng hợp các phần tử trong danh sách.

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
int sum = numbers.stream()
                 .reduce(0, Integer::sum);
System.out.println(sum); // Output: 15
```

#### 4. Xử lý Song song
Sử dụng `parallelStream` để xử lý dữ liệu song song.

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
int sum = numbers.parallelStream()
                 .reduce(0, Integer::sum);
System.out.println(sum); // Output: 15
```

Lập trình hướng hàm trong Java giúp viết code rõ ràng, dễ bảo trì và mở rộng, đồng thời cải thiện hiệu suất với khả năng xử lý song song mạnh mẽ. Các tính năng như lambda expressions, Stream API và functional interfaces làm cho việc áp dụng phong cách lập trình này trở nên dễ dàng và hiệu quả.

### Sử dụng Optional trong Java

`Optional` là một lớp trong Java 8 được sử dụng để tránh lỗi NullPointerException. Nó đại diện cho một giá trị có thể có hoặc không. `Optional` cung cấp các phương thức để kiểm tra và xử lý giá trị một cách an toàn.

### Các Tạo Optional

```java
// Tạo Optional từ một giá trị không null
Optional<String> nonEmptyOptional = Optional.of("Hello");

// Tạo Optional từ một giá trị có thể null
Optional<String> nullableOptional = Optional.ofNullable(null);

// Tạo một Optional rỗng
Optional<String> emptyOptional = Optional.empty();
```

### Các Phương thức của Optional

#### 1. Kiểm tra giá trị có tồn tại

```java
Optional<String> optional = Optional.of("Hello");

// Kiểm tra giá trị có tồn tại
if (optional.isPresent()) {
    System.out.println("Value is present");
}

// Sử dụng ifPresent để thực thi hành động nếu giá trị tồn tại
optional.ifPresent(value -> System.out.println("Value: " + value));
```

#### 2. Lấy giá trị

```java
Optional<String> optional = Optional.of("Hello");

// Lấy giá trị bằng get() (không nên sử dụng nếu không chắc chắn giá trị tồn tại)
String value = optional.get();
System.out.println("Value: " + value);

// Sử dụng orElse để cung cấp giá trị mặc định nếu không tồn tại
String defaultValue = optional.orElse("Default Value");
System.out.println("Value: " + defaultValue);

// Sử dụng orElseGet để cung cấp giá trị mặc định bằng một supplier
String defaultValueSupplier = optional.orElseGet(() -> "Default Value from Supplier");
System.out.println("Value: " + defaultValueSupplier);

// Sử dụng orElseThrow để ném ngoại lệ nếu không tồn tại giá trị
String valueOrException = optional.orElseThrow(() -> new IllegalArgumentException("Value not present"));
System.out.println("Value: " + valueOrException);
```

#### 3. Biến đổi Giá trị

```java
Optional<String> optional = Optional.of("Hello");

// Sử dụng map để biến đổi giá trị nếu tồn tại
Optional<Integer> lengthOptional = optional.map(String::length);
lengthOptional.ifPresent(length -> System.out.println("Length: " + length));

// Sử dụng flatMap để biến đổi giá trị và trả về Optional khác
Optional<String> optionalWithTransform = optional.flatMap(value -> Optional.of(value.toUpperCase()));
optionalWithTransform.ifPresent(value -> System.out.println("Transformed Value: " + value));
```

#### 4. Lọc giá trị

```java
Optional<String> optional = Optional.of("Hello");

// Sử dụng filter để lọc giá trị
Optional<String> filteredOptional = optional.filter(value -> value.startsWith("H"));
filteredOptional.ifPresent(value -> System.out.println("Filtered Value: " + value));
```

### Các Tình huống Thường gặp

#### 1. Xử lý Kết quả từ Phương thức có thể Trả về null

```java
public class User {
    private String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

public Optional<User> findUserById(String id) {
    if ("123".equals(id)) {
        return Optional.of(new User("John"));
    }
    return Optional.empty();
}

public void printUserName(String id) {
    Optional<User> userOptional = findUserById(id);
    userOptional.ifPresent(user -> System.out.println("User Name: " + user.getName()));
}

public static void main(String[] args) {
    printUserName("123"); // Output: User Name: John
    printUserName("456"); // No output
}
```

#### 2. Kết hợp nhiều Optional

```java
Optional<String> firstNameOptional = Optional.of("John");
Optional<String> lastNameOptional = Optional.of("Doe");

Optional<String> fullNameOptional = firstNameOptional.flatMap(firstName ->
        lastNameOptional.map(lastName -> firstName + " " + lastName));

fullNameOptional.ifPresent(fullName -> System.out.println("Full Name: " + fullName));
// Output: Full Name: John Doe
```

### Kết luận

`Optional` là một công cụ mạnh mẽ trong Java giúp bạn xử lý các giá trị có thể null một cách an toàn và rõ ràng. Việc sử dụng `Optional` giúp giảm thiểu lỗi NullPointerException và làm cho mã nguồn dễ đọc và bảo trì hơn.