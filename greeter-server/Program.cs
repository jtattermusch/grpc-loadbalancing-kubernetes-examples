// Copyright 2015 gRPC authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using System;
using System.Threading.Tasks;
using Grpc.Core;
using Helloworld;

namespace GreeterServer
{
    class GreeterImpl : Greeter.GreeterBase
    {
        // Server side handler of the SayHello RPC
        public override Task<HelloReply> SayHello(HelloRequest request, ServerCallContext context)
        {
            return Task.FromResult(new HelloReply { Message = "Hello " + request.Name + " (Backend IP: " +  Environment.GetEnvironmentVariable("MY_POD_IP") + ")" });
        }
    }

    class Program
    {
        const int Port = 8000;

        public static void Main(string[] args)
        {
            // Work around to make round_robin LB policy re-resolve backends periodically and thus pick up newly deployed replicas of the service.
            // see https://github.com/grpc/proposal/blob/master/A9-server-side-conn-mgt.md
            var serverArgs = new[] {new ChannelOption("grpc.max_connection_age_ms", 5000), new ChannelOption("grpc.max_connection_age_grace_ms", 3000)};

            Server server = new Server(serverArgs)
            {
                Services = { Greeter.BindService(new GreeterImpl()) },
                Ports = { new ServerPort("0.0.0.0", Port, ServerCredentials.Insecure) },
            };
            server.Start();
            Console.WriteLine("Started server on port " + Port);

            // wait forever
            server.ShutdownTask.Wait();
        }
    }
}
