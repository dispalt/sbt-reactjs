/** @jsx React.DOM */
var MoreMessage = React.createClass({
  render () {
    return <div>{this.props.names.map((name) => <span>{name}</span>)}</div>;
  }
});

React.renderComponent(<MoreMessage names={["John", "Mary"]} />, mountNode);
