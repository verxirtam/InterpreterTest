


interface MessageContext
{
	String nextToken();
	String skipToken(String skipString);
	String skipReturn();
	boolean hasNextToken();
}

interface MessageProcedure
{
	void process(MessageContext context) throws Exception;
}

class Message implements MessageProcedure
{
	@Override
	public void process(MessageContext context) throws Exception
	{
		context.skipToken("MASSAGE_1.0");
		context.skipReturn();
		MessageBody mb=new MessageBody();
		mb.process(context);
	}
}

class MessageBody implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		context.skipToken("EV3LineTracer_1.0");
		context.skipReturn();
		Command com=new Command();
		com.process(context);
	}
	
}

class Command implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		String commandstring = context.nextToken();
		context.skipReturn();
		
		createCommand(commandstring).process(context);
		
		
	}
	MessageProcedure createCommand(String commandstring) throws Exception
	{
		if(commandstring.equals("SetMDP"))
		{
			return new CommandSetMDP();
		}
		if(commandstring.equals("ExecEpisode"))
		{
			return new CommandExecEpisode();
		}
		throw new Exception(this.getClass().getName());
	}
}

class CommandSetMDP implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		//コマンド毎の処理を実装する
		
	}
	
}
class CommandExecEpisode implements MessageProcedure
{

	@Override
	public void process(MessageContext context) throws Exception
	{
		//コマンド毎の処理を実装する
	}
	
}
public class InterpreterTest
{

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
	}

}
