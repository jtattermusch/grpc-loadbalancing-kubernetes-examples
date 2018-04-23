FROM microsoft/dotnet:2.0-sdk
WORKDIR /app

COPY . /app

RUN dotnet publish -o out -c Release -f netcoreapp2.0 GreeterClient.csproj

ENTRYPOINT ["dotnet", "exec", "/app/out/GreeterClient.dll"]
