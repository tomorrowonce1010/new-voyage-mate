#!/bin/bash

echo "=== 最终配置检查 ==="

echo "1. 检查application.properties..."
if [ -f "src/main/resources/application.properties" ]; then
    echo "✅ application.properties存在"
    grep -E "(url|username|password)" src/main/resources/application.properties
else
    echo "❌ application.properties不存在"
fi

echo ""
echo "2. 测试数据库连接..."
# 根据配置测试连接
USERNAME=$(grep "spring.datasource.username" src/main/resources/application.properties | cut -d'=' -f2-)
PASSWORD=$(grep "spring.datasource.password" src/main/resources/application.properties | cut -d'=' -f2-)

if [ -z "$PASSWORD" ]; then
    mysql -u $USERNAME -e "SELECT '✅ 连接成功' as status;"
else
    mysql -u $USERNAME -p"$PASSWORD" -e "SELECT '✅ 连接成功' as status;"
fi

if [ $? -eq 0 ]; then
    echo "✅ 数据库连接正常"
    echo ""
    echo "🎉 现在可以启动Spring Boot应用了!"
    echo "运行: mvn spring-boot:run -DskipTests"
else
    echo "❌ 数据库连接失败，请检查配置"
fi
